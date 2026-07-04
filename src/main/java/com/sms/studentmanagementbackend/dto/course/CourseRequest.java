package com.sms.studentmanagementbackend.dto.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CourseRequest(
        @NotBlank(message = "Course code is required")
        String courseCode,
        @NotBlank(message = "Course name is required")
        String courseName,
        @NotNull(message = "Credits are required")
        @Min(value = 1, message = "Credits must be at least 1")
        @Max(value = 10, message = "Credits cannot exceed 10")
        Integer credits,
        @NotNull(message = "Semester is required")
        @Min(value = 1, message = "Semester must be at least 1")
        @Max(value = 12, message = "Semester cannot exceed 12")
        Integer semester,
        @NotBlank(message = "Department is required")
        String department,
        Set<Long> facultyIds,
        Set<Long> studentIds
) {
}
