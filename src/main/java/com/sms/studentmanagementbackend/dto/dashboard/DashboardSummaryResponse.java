package com.sms.studentmanagementbackend.dto.dashboard;

import java.util.Map;
import lombok.Builder;

@Builder
public record DashboardSummaryResponse(
        long totalStudents,
        long totalCourses,
        long totalFaculty,
        long todaysAttendance,
        long presentCount,
        long absentCount,
        double todaysAttendancePercentage,
        long activeCourses,
        Map<String, Long> studentCountByDepartment,
        Map<String, Long> courseDistributionByDepartment
) {
}
