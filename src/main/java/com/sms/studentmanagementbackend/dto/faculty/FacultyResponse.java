package com.sms.studentmanagementbackend.dto.faculty;

import com.sms.studentmanagementbackend.dto.course.CourseSummaryResponse;
import com.sms.studentmanagementbackend.entity.enums.FacultyStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record FacultyResponse(
        Long id,
        String facultyId,
        String name,
        String email,
        String phone,
        String department,
        String qualification,
        String designation,
        FacultyStatus status,
        List<CourseSummaryResponse> courses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
