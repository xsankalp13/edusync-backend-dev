package com.project.edusync.hrms.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record HrmsDashboardSummaryDTO(
        int totalActiveStaff,
        int staffWithSalaryMapping,
        int staffWithoutSalaryMapping,
        BigDecimal totalPayrollThisMonth,
        BigDecimal totalPayrollLastMonth,
        int pendingLeaveApplications,
        int todayPresent,
        int todayAbsent,
        int todayOnLeave,
        int totalTeachingStaff,
        int totalNonTeachingAdmin,
        int totalNonTeachingSupport,
        List<GradeDistributionItem> gradeDistribution,
        List<MonthlyPayrollTrendItem> payrollTrend,
        List<CategoryAttendanceItem> categoryAttendance
) {
}

