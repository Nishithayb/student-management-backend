package com.sms.studentmanagementbackend.dto.attendance;

import com.sms.studentmanagementbackend.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AttendanceRequest(
        @NotNull(message = "Student ID is required")
        Long studentId,
        @NotNull(message = "Course ID is required")
        Long courseId,
        Long facultyId,
        @NotNull(message = "Attendance date is required")
        LocalDate attendanceDate,
        @NotNull(message = "Attendance status is required")
        AttendanceStatus status,
        String remarks
) {
}
