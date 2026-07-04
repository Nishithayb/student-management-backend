package com.sms.studentmanagementbackend.dto.auth;

import com.sms.studentmanagementbackend.entity.enums.UserRole;
import lombok.Builder;

@Builder
public record RegistrationResponse(
        Long userId,
        String fullName,
        String username,
        String email,
        String phone,
        String employeeId,
        String department,
        UserRole role,
        String message
) {
}
