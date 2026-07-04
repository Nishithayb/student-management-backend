package com.sms.studentmanagementbackend.dto.student;

import lombok.Builder;

@Builder
public record StudentImportError(
        int rowNumber,
        String studentId,
        String message
) {
}
