package com.sms.studentmanagementbackend.dto.auth;

import com.sms.studentmanagementbackend.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 150, message = "Full name must be between 3 and 150 characters")
        @Pattern(regexp = "^[A-Za-z ]{3,150}$", message = "Full name must contain only alphabets and spaces")
        String fullName,
        @NotBlank(message = "Username is required")
        @Size(min = 5, max = 50, message = "Username must be between 5 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]{5,50}$", message = "Username must not contain spaces and may use letters, numbers, dots, underscores, and hyphens only")
        String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
        String phone,
        @NotBlank(message = "Employee ID is required")
        @Pattern(regexp = "^FAC\\d{4}$", message = "Employee ID must match the FAC0001 format")
        String employeeId,
        @NotBlank(message = "Department is required")
        String department,
        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
                message = "Password must contain upper, lower, digit, special character and be at least 8 characters"
        )
        String password,
        @NotNull(message = "Role is required")
        UserRole role
) {
}
