package com.sms.studentmanagementbackend.dto.attendance;

import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AttendanceResponse(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseName,
        Long facultyId,
        String facultyName,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
