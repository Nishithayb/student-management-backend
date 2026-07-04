package com.sms.studentmanagementbackend.dto.auth;

import com.sms.studentmanagementbackend.entity.enums.UserRole;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String username,
        String email,
        UserRole role
) {
}
