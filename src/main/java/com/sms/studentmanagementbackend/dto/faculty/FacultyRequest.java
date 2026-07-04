package com.sms.studentmanagementbackend.dto.faculty;

import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record FacultyRequest(
        @NotBlank(message = "Faculty ID is required")
        String facultyId,
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Phone number is invalid")
        String phone,
        @NotBlank(message = "Department is required")
        String department,
        @NotBlank(message = "Qualification is required")
        String qualification,
        @NotBlank(message = "Designation is required")
        String designation,
        @NotNull(message = "Status is required")
        FacultyStatus status,
        Set<Long> courseIds
) {
}
