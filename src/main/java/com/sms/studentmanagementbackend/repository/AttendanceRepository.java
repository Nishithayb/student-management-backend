package com.sms.studentmanagementbackend.repository;

import com.sms.studentmanagementbackend.entity.Attendance;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByStudentIdAndCourseIdAndAttendanceDate(Long studentId, Long courseId, LocalDate attendanceDate);

    long countByAttendanceDateAndStatus(LocalDate attendanceDate, com.sms.studentmanagementbackend.entity.enums.AttendanceStatus status);

    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);

    void deleteByFacultyId(Long facultyId);
}
