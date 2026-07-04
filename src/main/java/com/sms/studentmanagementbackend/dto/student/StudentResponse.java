package com.sms.studentmanagementbackend.dto.student;

import com.sms.studentmanagementbackend.dto.course.CourseSummaryResponse;
import com.sms.studentmanagementbackend.entity.enums.Gender;
import com.sms.studentmanagementbackend.entity.enums.StudentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record StudentResponse(
        Long id,
        String studentId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dob,
        Gender gender,
        String address,
        String department,
        Integer semester,
        LocalDate joiningDate,
        StudentStatus status,
        String imageUrl,
        List<CourseSummaryResponse> courses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
