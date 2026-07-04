package com.sms.studentmanagementbackend.dto.student;

import com.sms.studentmanagementbackend.entity.enums.Gender;
import com.sms.studentmanagementbackend.entity.enums.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Set;

public record StudentRequest(
        @NotBlank(message = "Student ID is required")
        String studentId,
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Phone number is invalid")
        String phone,
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dob,
        @NotNull(message = "Gender is required")
        Gender gender,
        @NotBlank(message = "Address is required")
        String address,
        @NotBlank(message = "Department is required")
        String department,
        @NotNull(message = "Semester is required")
        @Min(value = 1, message = "Semester must be at least 1")
        @Max(value = 12, message = "Semester cannot exceed 12")
        Integer semester,
        @NotNull(message = "Joining date is required")
        LocalDate joiningDate,
        @NotNull(message = "Status is required")
        StudentStatus status,
        String imageUrl,
        Set<Long> courseIds
) {
}
