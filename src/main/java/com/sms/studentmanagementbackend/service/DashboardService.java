package com.sms.studentmanagementbackend.service;

import com.sms.studentmanagementbackend.dto.dashboard.DashboardSummaryResponse;
import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import com.sms.studentmanagementbackend.repository.AttendanceRepository;
import com.sms.studentmanagementbackend.repository.CourseRepository;
import com.sms.studentmanagementbackend.repository.FacultyRepository;
import com.sms.studentmanagementbackend.repository.StudentRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardSummaryResponse getSummary() {
        long totalStudents = studentRepository.count();
        long totalCourses = courseRepository.count();
        long totalFaculty = facultyRepository.count();
        LocalDate today = LocalDate.now();
        long presentCount = attendanceRepository.countByAttendanceDateAndStatus(today, AttendanceStatus.PRESENT);
        long absentCount = attendanceRepository.countByAttendanceDateAndStatus(today, AttendanceStatus.ABSENT);
        long todaysAttendance = presentCount + absentCount;
        double attendancePercentage = todaysAttendance == 0 ? 0.0 : (presentCount * 100.0) / todaysAttendance;

        Map<String, Long> studentCountByDepartment = studentRepository.findAll().stream()
                .collect(Collectors.groupingBy(student -> student.getDepartment().trim(), Collectors.counting()));
        Map<String, Long> courseDistributionByDepartment = courseRepository.findAll().stream()
                .collect(Collectors.groupingBy(course -> course.getDepartment().trim(), Collectors.counting()));

        return DashboardSummaryResponse.builder()
                .totalStudents(totalStudents)
                .totalCourses(totalCourses)
                .totalFaculty(totalFaculty)
                .todaysAttendance(todaysAttendance)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .todaysAttendancePercentage(Math.round(attendancePercentage * 100.0) / 100.0)
                .activeCourses(totalCourses)
                .studentCountByDepartment(studentCountByDepartment)
                .courseDistributionByDepartment(courseDistributionByDepartment)
                .build();
    }
}
