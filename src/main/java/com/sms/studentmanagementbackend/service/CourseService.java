package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.course.CourseRequest;
import com.sms.studentmanagementbackend.dto.course.CourseResponse;
import com.sms.studentmanagementbackend.dto.faculty.FacultySummaryResponse;
import com.sms.studentmanagementbackend.dto.student.StudentSummaryResponse;
import com.sms.studentmanagementbackend.entity.Course;
import com.sms.studentmanagementbackend.entity.Faculty;
import com.sms.studentmanagementbackend.entity.Student;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.exception.DuplicateResourceException;
import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyRepository;
import com.sms.studentmanagementbackend.repository.StudentRepository;
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
public class CourseService {

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getCourses(
            String q,
            String department,
            Integer semester,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageUtils.createPageable(page, size, sortBy, sortDir);
        Page<CourseResponse> result = courseRepository.findAll(buildSpecification(q, department, semester), pageable).map(this::toResponse);
        return PageUtils.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        return toResponse(findCourse(id));
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByCourseCode(request.courseCode())) {
            throw new DuplicateResourceException("Course code already exists");
        }
        Course course = new Course();
        applyRequest(course, request);
        courseRepository.save(course);
        auditLogService.log(
                AuditAction.CREATE,
                "COURSE",
                String.valueOf(course.getId()),
                "Created course " + course.getCourseCode() + " (" + course.getCourseName() + ")"
        );
        return toResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourse(id);
        if (courseRepository.existsByCourseCodeAndIdNot(request.courseCode(), id)) {
            throw new DuplicateResourceException("Course code already exists");
        }
        applyRequest(course, request);
        courseRepository.save(course);
        auditLogService.log(
                AuditAction.UPDATE,
                "COURSE",
                String.valueOf(course.getId()),
                "Updated course " + course.getCourseCode() + " (" + course.getCourseName() + ")"
        );
        return toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = findCourse(id);
        attendanceRepository.deleteByCourseId(id);
        new java.util.HashSet<>(course.getFacultyMembers()).forEach(faculty -> faculty.removeCourse(course));
        course.getStudents().forEach(student -> student.getCourses().remove(course));
        course.getFacultyAssignments().clear();
        course.getStudents().clear();
        courseRepository.save(course);
        courseRepository.delete(course);
        auditLogService.log(
                AuditAction.DELETE,
                "COURSE",
                String.valueOf(id),
                "Deleted course " + course.getCourseCode() + " (" + course.getCourseName() + ")"
        );
    }

    @Transactional(readOnly = true)
    public List<FacultySummaryResponse> getAssignedFaculty(Long courseId) {
        Course course = findCourse(courseId);
        return course.getFacultyMembers().stream()
                .map(faculty -> new FacultySummaryResponse(faculty.getId(), faculty.getFacultyId(), faculty.getName()))
                .sorted(java.util.Comparator.comparing(FacultySummaryResponse::facultyId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSummaryResponse> getEnrolledStudents(Long courseId) {
        Course course = findCourse(courseId);
        return course.getStudents().stream()
                .map(student -> new StudentSummaryResponse(student.getId(), student.getStudentId(), student.getFirstName() + " " + student.getLastName()))
                .sorted(java.util.Comparator.comparing(StudentSummaryResponse::studentId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportCoursesToExcel(
            String q,
            String department,
            Integer semester,
            String sortBy,
            String sortDir
    ) {
        List<Course> courses = findCourses(q, department, semester, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "COURSE_EXPORT", null, "Exported " + courses.size() + " courses to Excel");
        return ExportUtils.toExcel("Courses", courseHeaders(), toRows(courses));
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportCoursesToPdf(
            String q,
            String department,
            Integer semester,
            String sortBy,
            String sortDir
    ) {
        List<Course> courses = findCourses(q, department, semester, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "COURSE_EXPORT", null, "Exported " + courses.size() + " courses to PDF");
        return ExportUtils.toPdf("Courses Report", courseHeaders(), toRows(courses));
    }

    private Specification<Course> buildSpecification(String q, String department, Integer semester) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (q != null && !q.isBlank()) {
                String term = "%" + q.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("courseCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("courseName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), term)
                ));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("department")), department.trim().toLowerCase()));
            }
            if (semester != null) {
                predicates.add(criteriaBuilder.equal(root.get("semester"), semester));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Course> findCourses(String q, String department, Integer semester, String sortBy, String sortDir) {
        Sort sort = PageUtils.createSort(sortBy, sortDir);
        return courseRepository.findAll(buildSpecification(q, department, semester), sort);
    }

    private void applyRequest(Course course, CourseRequest request) {
        course.setCourseCode(request.courseCode());
        course.setCourseName(request.courseName());
        course.setCredits(request.credits());
        course.setSemester(request.semester());
        course.setDepartment(request.department());

        Set<Faculty> facultyMembers = request.facultyIds() == null || request.facultyIds().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(facultyRepository.findAllById(request.facultyIds()));
        if (request.facultyIds() != null && facultyMembers.size() != request.facultyIds().size()) {
            throw new NotFoundException("One or more faculty records were not found");
        }

        Set<Student> students = request.studentIds() == null || request.studentIds().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(studentRepository.findAllById(request.studentIds()));
        if (request.studentIds() != null && students.size() != request.studentIds().size()) {
            throw new NotFoundException("One or more student records were not found");
        }

        course.getFacultyAssignments().clear();
        for (Faculty faculty : facultyMembers) {
            faculty.assignCourse(course, java.time.LocalDate.now());
        }
        course.setStudents(students);
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Course not found"));
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .credits(course.getCredits())
                .semester(course.getSemester())
                .department(course.getDepartment())
                .facultyIds(course.getFacultyMembers().stream().map(Faculty::getId).sorted().toList())
                .studentIds(course.getStudents().stream().map(Student::getId).sorted().toList())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private List<String> courseHeaders() {
        return List.of("Course Code", "Course Name", "Credits", "Semester", "Department", "Assigned Faculty", "Enrolled Students");
    }

    private List<List<String>> toRows(List<Course> courses) {
        return courses.stream()
                .map(course -> List.of(
                        safe(course.getCourseCode()),
                        safe(course.getCourseName()),
                        String.valueOf(course.getCredits()),
                        String.valueOf(course.getSemester()),
                        safe(course.getDepartment()),
                        course.getFacultyMembers().stream().map(Faculty::getFacultyId).sorted().reduce((a, b) -> a + ", " + b).orElse("—"),
                        String.valueOf(course.getStudents().size())
                ))
                .toList();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
