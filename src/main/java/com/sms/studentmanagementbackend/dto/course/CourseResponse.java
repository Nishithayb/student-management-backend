package com.sms.studentmanagementbackend.dto.course;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record CourseResponse(
        Long id,
        String courseCode,
        String courseName,
        Integer credits,
        Integer semester,
        String department,
        List<Long> facultyIds,
        List<Long> studentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
