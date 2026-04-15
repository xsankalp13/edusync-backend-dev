package com.project.edusync.hrms.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
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
    // Phase 1 additions
    private AttendanceHeatmapDTO currentMonthHeatmap;
    private int pendingApprovalRequests;
}

