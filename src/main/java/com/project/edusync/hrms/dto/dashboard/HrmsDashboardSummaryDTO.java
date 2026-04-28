package com.project.edusync.hrms.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmsDashboardSummaryDTO {
    private int totalActiveStaff;
    private int staffWithSalaryMapping;
    private int staffWithoutSalaryMapping;
    private BigDecimal totalPayrollThisMonth;
    private BigDecimal totalPayrollLastMonth;
    private int pendingLeaveApplications;
    private int todayPresent;
    private int todayAbsent;
    private int todayOnLeave;
    private int totalTeachingStaff;
    private int totalNonTeachingAdmin;
    private int totalNonTeachingSupport;
    private List<GradeDistributionItem> gradeDistribution;
    private List<MonthlyPayrollTrendItem> payrollTrend;
    private List<CategoryAttendanceItem> categoryAttendance;
    // Phase 1 — removed currentMonthHeatmap: now fetched via dedicated /attendance-heatmap endpoint
    private int pendingApprovalRequests;
    // Phase 5 — Dashboard Intelligence
    private int pendingProxyCount;
    private int pendingLateClockInCount;
    private double staffPresentPercent;
}
