package com.sms.studentmanagementbackend.dto.course;

public record CourseSummaryResponse(
        Long id,
        String courseCode,
        String courseName
) {
}
