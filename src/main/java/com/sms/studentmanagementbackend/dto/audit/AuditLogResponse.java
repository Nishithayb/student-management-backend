package com.sms.studentmanagementbackend.dto.audit;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AuditLogResponse(
        Long id,
        String username,
        String role,
        String action,
        String entityName,
        String entityId,
        String description,
        String ipAddress,
        LocalDateTime timestamp
) {
}
