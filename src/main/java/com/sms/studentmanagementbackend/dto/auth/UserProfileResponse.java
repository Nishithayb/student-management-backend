package com.sms.studentmanagementbackend.dto.auth;

import com.sms.studentmanagementbackend.entity.enums.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserProfileResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String employeeId,
        String department,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
}
