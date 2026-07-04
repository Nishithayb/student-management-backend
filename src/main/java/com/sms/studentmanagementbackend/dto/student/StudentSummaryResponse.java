package com.sms.studentmanagementbackend.dto.student;

public record StudentSummaryResponse(
        Long id,
        String studentId,
        String name
) {
}
