package com.sms.studentmanagementbackend.dto.attendance;

import lombok.Builder;

@Builder
public record AttendanceReportResponse(
        String scope,
        Long referenceId,
        String referenceName,
        long totalSessions,
        long presentCount,
        long absentCount,
        double attendancePercentage
) {
}
