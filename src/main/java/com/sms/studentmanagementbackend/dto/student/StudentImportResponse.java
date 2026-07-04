package com.sms.studentmanagementbackend.dto.student;

import java.util.List;
import lombok.Builder;

@Builder
public record StudentImportResponse(
        int totalRows,
        int importedCount,
        int failedCount,
        int duplicateCount,
        List<StudentImportError> errors
) {
}
