package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.PageResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceMonthlyReportResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceReportResponse;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceRequest;
import com.sms.studentmanagementbackend.dto.attendance.AttendanceResponse;
import com.sms.studentmanagementbackend.entity.Attendance;
import com.sms.studentmanagementbackend.entity.Course;
import com.sms.studentmanagementbackend.entity.Faculty;
import com.sms.studentmanagementbackend.entity.Student;
import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import com.sms.studentmanagementbackend.entity.enums.AuditAction;
import com.sms.studentmanagementbackend.exception.NotFoundException;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyRepository;
import com.sms.studentmanagementbackend.repository.StudentRepository;
import com.sms.studentmanagementbackend.util.ExportUtils;
import com.sms.studentmanagementbackend.util.PageUtils;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(
                        request.studentId(),
                        request.courseId(),
                        request.attendanceDate()
                )
                .orElseGet(Attendance::new);

        boolean existing = attendance.getId() != null;
        applyRequest(attendance, request);
        attendanceRepository.save(attendance);

        auditLogService.log(
                existing ? AuditAction.UPDATE : AuditAction.CREATE,
                "ATTENDANCE",
                String.valueOf(attendance.getId()),
                (existing ? "Updated" : "Marked") + " attendance for student " + attendance.getStudent().getStudentId()
                        + " in course " + attendance.getCourse().getCourseCode() + " on " + attendance.getAttendanceDate()
        );
        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long attendanceId, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Attendance record not found"));
        applyRequest(attendance, request);
        attendanceRepository.save(attendance);
        auditLogService.log(
                AuditAction.UPDATE,
                "ATTENDANCE",
                String.valueOf(attendance.getId()),
                "Updated attendance record " + attendance.getId()
        );
        return toResponse(attendance);
    }

    @Transactional
    public void deleteAttendance(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Attendance record not found"));
        attendanceRepository.delete(attendance);
        auditLogService.log(
                AuditAction.DELETE,
                "ATTENDANCE",
                String.valueOf(attendanceId),
                "Deleted attendance record " + attendanceId
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getAttendance(
            Long studentId,
            Long courseId,
            Long facultyId,
            AttendanceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = PageUtils.createPageable(page, size, sortBy, sortDir);
        Page<AttendanceResponse> result = attendanceRepository.findAll(
                        buildSpecification(studentId, courseId, facultyId, status, fromDate, toDate),
                        pageable
                )
                .map(this::toResponse);
        return PageUtils.toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getAttendanceByStudent(
            Long studentId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        return getAttendance(studentId, null, null, null, fromDate, toDate, page, size, sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse getStudentReport(Long studentId, LocalDate fromDate, LocalDate toDate) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        List<Attendance> records = findAttendance(studentId, null, null, null, fromDate, toDate, "attendanceDate", "desc");
        return summarize("STUDENT", studentId, student.getFirstName() + " " + student.getLastName(), records);
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse getCourseReport(Long courseId, LocalDate fromDate, LocalDate toDate) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found"));
        List<Attendance> records = findAttendance(null, courseId, null, null, fromDate, toDate, "attendanceDate", "desc");
        return summarize("COURSE", courseId, course.getCourseCode() + " - " + course.getCourseName(), records);
    }

    @Transactional(readOnly = true)
    public AttendanceMonthlyReportResponse getMonthlyReport(int month, int year, Long courseId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate fromDate = yearMonth.atDay(1);
        LocalDate toDate = yearMonth.atEndOfMonth();
        List<Attendance> records = findAttendance(null, courseId, null, null, fromDate, toDate, "attendanceDate", "asc");

        Map<String, List<Attendance>> grouped = new LinkedHashMap<>();
        for (Attendance record : records) {
            String key = record.getCourse().getCourseCode() + " - " + record.getCourse().getCourseName();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }

        List<AttendanceReportResponse> summaries = new ArrayList<>();
        grouped.forEach((name, values) -> summaries.add(summarize("MONTHLY_COURSE", courseId, name, values)));
        return AttendanceMonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .records(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportAttendanceToExcel(
            Long studentId,
            Long courseId,
            Long facultyId,
            AttendanceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        List<Attendance> attendance = findAttendance(studentId, courseId, facultyId, status, fromDate, toDate, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "ATTENDANCE_EXPORT", null, "Exported " + attendance.size() + " attendance records to Excel");
        return ExportUtils.toExcel("Attendance", headers(), toRows(attendance));
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportAttendanceToPdf(
            Long studentId,
            Long courseId,
            Long facultyId,
            AttendanceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        List<Attendance> attendance = findAttendance(studentId, courseId, facultyId, status, fromDate, toDate, sortBy, sortDir);
        auditLogService.log(AuditAction.CREATE, "ATTENDANCE_EXPORT", null, "Exported " + attendance.size() + " attendance records to PDF");
        return ExportUtils.toPdf("Attendance Report", headers(), toRows(attendance));
    }

    private void applyRequest(Attendance attendance, AttendanceRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Student not found"));
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found"));
        Faculty faculty = request.facultyId() == null ? null : facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> new NotFoundException("Faculty not found"));

        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setFaculty(faculty);
        attendance.setAttendanceDate(request.attendanceDate());
        attendance.setStatus(request.status());
        attendance.setRemarks(request.remarks());
    }

    private Specification<Attendance> buildSpecification(
            Long studentId,
            Long courseId,
            Long facultyId,
            AttendanceStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("student").get("id"), studentId));
            }
            if (courseId != null) {
                predicates.add(criteriaBuilder.equal(root.get("course").get("id"), courseId));
            }
            if (facultyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("faculty").get("id"), facultyId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("attendanceDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("attendanceDate"), toDate));
            }
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Attendance> findAttendance(
            Long studentId,
            Long courseId,
            Long facultyId,
            AttendanceStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir
    ) {
        Sort sort = PageUtils.createSort(sortBy, sortDir);
        return attendanceRepository.findAll(buildSpecification(studentId, courseId, facultyId, status, fromDate, toDate), sort);
    }

    private AttendanceReportResponse summarize(String scope, Long referenceId, String referenceName, List<Attendance> records) {
        long presentCount = records.stream().filter(record -> record.getStatus() == AttendanceStatus.PRESENT).count();
        long absentCount = records.stream().filter(record -> record.getStatus() == AttendanceStatus.ABSENT).count();
        long total = records.size();
        double percentage = total == 0 ? 0.0 : Math.round((presentCount * 10000.0) / total) / 100.0;

        return AttendanceReportResponse.builder()
                .scope(scope)
                .referenceId(referenceId)
                .referenceName(referenceName)
                .totalSessions(total)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .attendancePercentage(percentage)
                .build();
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName())
                .courseId(attendance.getCourse().getId())
                .courseName(attendance.getCourse().getCourseName())
                .facultyId(attendance.getFaculty() == null ? null : attendance.getFaculty().getId())
                .facultyName(attendance.getFaculty() == null ? null : attendance.getFaculty().getName())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }

    private List<String> headers() {
        return List.of("Date", "Student ID", "Student Name", "Course", "Faculty", "Status", "Remarks");
    }

    private List<List<String>> toRows(List<Attendance> attendance) {
        return attendance.stream()
                .map(item -> List.of(
                        String.valueOf(item.getAttendanceDate()),
                        item.getStudent().getStudentId(),
                        item.getStudent().getFirstName() + " " + item.getStudent().getLastName(),
                        item.getCourse().getCourseCode() + " - " + item.getCourse().getCourseName(),
                        item.getFaculty() == null ? "—" : item.getFaculty().getFacultyId() + " - " + item.getFaculty().getName(),
                        item.getStatus().name(),
                        item.getRemarks() == null || item.getRemarks().isBlank() ? "—" : item.getRemarks()
                ))
                .toList();
    }
}
