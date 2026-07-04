package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.course.CourseSummaryResponse;
import com.sms.studentmanagementbackend.dto.faculty.FacultyRequest;
import com.sms.studentmanagementbackend.dto.faculty.FacultyResponse;
import com.sms.studentmanagementbackend.entity.Course;
import com.sms.studentmanagementbackend.entity.Faculty;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import com.sms.studentmanagementbackend.exception.DuplicateResourceException;
import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyCourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyRepository;
import com.sms.studentmanagementbackend.util.ExportUtils;
import com.sms.studentmanagementbackend.util.PageUtils;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final FacultyCourseRepository facultyCourseRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<FacultyResponse> getFaculty(
            String q,
            String department,
            FacultyStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageUtils.createPageable(page, size, sortBy, sortDir);
        Page<FacultyResponse> result = facultyRepository.findAll(buildSpecification(q, department, status), pageable).map(this::toResponse);
        return PageUtils.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public FacultyResponse getFacultyById(Long id) {
        return toResponse(findFaculty(id));
    }

    @Transactional
    public FacultyResponse createFaculty(FacultyRequest request) {
        if (facultyRepository.existsByFacultyId(request.facultyId())) {
            throw new DuplicateResourceException("Faculty ID already exists");
        }
        if (facultyRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Faculty email already exists");
        }
        Faculty faculty = new Faculty();
        applyBasicFields(faculty, request);
        facultyRepository.save(faculty);
        syncAssignedCourses(faculty, request.courseIds());
        auditLogService.log(
                AuditAction.CREATE,
                "FACULTY",
                String.valueOf(faculty.getId()),
                "Created faculty " + faculty.getFacultyId() + " (" + faculty.getName() + ")"
        );
        return toResponse(faculty);
    }

    @Transactional
    public FacultyResponse updateFaculty(Long id, FacultyRequest request) {
        Faculty faculty = findFaculty(id);
        if (facultyRepository.existsByFacultyIdAndIdNot(request.facultyId(), id)) {
            throw new DuplicateResourceException("Faculty ID already exists");
        }
        if (facultyRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Faculty email already exists");
        }
        applyBasicFields(faculty, request);
        facultyRepository.save(faculty);
        syncAssignedCourses(faculty, request.courseIds());
        auditLogService.log(
                AuditAction.UPDATE,
                "FACULTY",
                String.valueOf(faculty.getId()),
                "Updated faculty " + faculty.getFacultyId() + " (" + faculty.getName() + ")"
        );
        return toResponse(faculty);
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty faculty = findFaculty(id);
        attendanceRepository.deleteByFacultyId(id);
        Set<Course> courses = new HashSet<>(faculty.getCourses());
        for (Course course : courses) {
            faculty.removeCourse(course);
            courseRepository.save(course);
        }
        facultyRepository.delete(faculty);
        auditLogService.log(
                AuditAction.DELETE,
                "FACULTY",
                String.valueOf(id),
                "Deleted faculty " + faculty.getFacultyId() + " (" + faculty.getName() + ")"
        );
    }

    @Transactional
    public FacultyResponse assignCourse(Long facultyId, Long courseId) {
        Faculty faculty = findFaculty(facultyId);
        Course course = findCourse(courseId);
        if (faculty.getCourses().contains(course)) {
            throw new DuplicateResourceException("Course is already assigned to this faculty member");
        }

        faculty.assignCourse(course, java.time.LocalDate.now());
        courseRepository.save(course);

        auditLogService.log(
                AuditAction.UPDATE,
                "FACULTY_COURSE_ASSIGNMENT",
                facultyId + ":" + courseId,
                "Assigned course " + course.getCourseCode() + " to faculty " + faculty.getFacultyId()
        );
        return toResponse(faculty);
    }

    @Transactional
    public FacultyResponse removeCourseAssignment(Long facultyId, Long courseId) {
        Faculty faculty = findFaculty(facultyId);
        Course course = findCourse(courseId);
        facultyCourseRepository.findByFacultyIdAndCourseId(facultyId, courseId)
                .orElseThrow(() -> new NotFoundException("Course assignment not found"));
        faculty.removeCourse(course);
        courseRepository.save(course);

        auditLogService.log(
                AuditAction.DELETE,
                "FACULTY_COURSE_ASSIGNMENT",
                facultyId + ":" + courseId,
                "Removed course " + course.getCourseCode() + " from faculty " + faculty.getFacultyId()
        );
        return toResponse(faculty);
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryResponse> getAssignedCourses(Long facultyId) {
        Faculty faculty = findFaculty(facultyId);
        return faculty.getCourses().stream()
                .map(course -> new CourseSummaryResponse(course.getId(), course.getCourseCode(), course.getCourseName()))
                .sorted(java.util.Comparator.comparing(CourseSummaryResponse::courseCode))
                .toList();
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportFacultyToExcel(
            String q,
            String department,
            FacultyStatus status,
            String sortBy,
            String sortDir
    ) {
        List<Faculty> faculty = findFacultyRecords(q, department, status, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "FACULTY_EXPORT", null, "Exported " + faculty.size() + " faculty records to Excel");
        return ExportUtils.toExcel("Faculty", facultyHeaders(), toRows(faculty));
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportFacultyToPdf(
            String q,
            String department,
            FacultyStatus status,
            String sortBy,
            String sortDir
    ) {
        List<Faculty> faculty = findFacultyRecords(q, department, status, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "FACULTY_EXPORT", null, "Exported " + faculty.size() + " faculty records to PDF");
        return ExportUtils.toPdf("Faculty Report", facultyHeaders(), toRows(faculty));
    }

    private Specification<Faculty> buildSpecification(String q, String department, FacultyStatus status) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("facultyId")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("designation")), term)
                ));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("department")), department.trim().toLowerCase()));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Faculty> findFacultyRecords(
            String q,
            String department,
            FacultyStatus status,
            String sortBy,
            String sortDir
    ) {
        Sort sort = PageUtils.createSort(sortBy, sortDir);
        return facultyRepository.findAll(buildSpecification(q, department, status), sort);
    }

    private void applyBasicFields(Faculty faculty, FacultyRequest request) {
        faculty.setFacultyId(request.facultyId());
        faculty.setName(request.name());
        faculty.setEmail(request.email());
        faculty.setPhone(request.phone());
        faculty.setDepartment(request.department());
        faculty.setQualification(request.qualification());
        faculty.setDesignation(request.designation());
        faculty.setStatus(request.status());
    }

    private void syncAssignedCourses(Faculty faculty, Set<Long> courseIds) {
        Set<Course> selectedCourses = courseIds == null || courseIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(courseRepository.findAllById(courseIds));
        if (courseIds != null && selectedCourses.size() != courseIds.size()) {
            throw new NotFoundException("One or more courses were not found");
        }

        Set<Course> existingCourses = new HashSet<>(faculty.getCourses());
        for (Course course : existingCourses) {
            faculty.removeCourse(course);
            courseRepository.save(course);
        }

        for (Course course : selectedCourses) {
            faculty.assignCourse(course, java.time.LocalDate.now());
            courseRepository.save(course);
        }
    }

    private Faculty findFaculty(Long id) {
        return facultyRepository.findById(id).orElseThrow(() -> new NotFoundException("Faculty not found"));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private FacultyResponse toResponse(Faculty faculty) {
        return FacultyResponse.builder()
                .id(faculty.getId())
                .facultyId(faculty.getFacultyId())
                .name(faculty.getName())
                .email(faculty.getEmail())
                .phone(faculty.getPhone())
                .department(faculty.getDepartment())
                .qualification(faculty.getQualification())
                .designation(faculty.getDesignation())
                .status(faculty.getStatus())
                .courses(faculty.getCourses().stream()
                        .map(course -> new CourseSummaryResponse(course.getId(), course.getCourseCode(), course.getCourseName()))
                        .sorted(java.util.Comparator.comparing(CourseSummaryResponse::courseCode))
                        .toList())
                .createdAt(faculty.getCreatedAt())
                .updatedAt(faculty.getUpdatedAt())
                .build();
    }

    private List<String> facultyHeaders() {
        return List.of("Faculty ID", "Name", "Email", "Phone", "Department", "Qualification", "Designation", "Status", "Assigned Courses");
    }

    private List<List<String>> toRows(List<Faculty> facultyMembers) {
        return facultyMembers.stream()
                .map(faculty -> List.of(
                        safe(faculty.getFacultyId()),
                        safe(faculty.getName()),
                        safe(faculty.getEmail()),
                        safe(faculty.getPhone()),
                        safe(faculty.getDepartment()),
                        safe(faculty.getQualification()),
                        safe(faculty.getDesignation()),
                        faculty.getStatus().name(),
                        faculty.getCourses().stream().map(Course::getCourseCode).sorted().reduce((a, b) -> a + ", " + b).orElse("—")
                ))
                .toList();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
