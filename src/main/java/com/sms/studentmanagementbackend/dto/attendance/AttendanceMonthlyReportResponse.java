package com.sms.studentmanagementbackend.dto.attendance;

import java.util.List;
import lombok.Builder;

@Builder
public record AttendanceMonthlyReportResponse(
        int month,
        int year,
        List<AttendanceReportResponse> records
) {
}
