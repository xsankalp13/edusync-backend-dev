package com.project.edusync.hrms.service.impl;

import com.project.edusync.ams.model.enums.LateClockInStatus;
import com.project.edusync.ams.model.repository.LateClockInRequestRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.hrms.dto.dashboard.AttendanceHeatmapDTO;
import com.project.edusync.hrms.dto.dashboard.CategoryAttendanceItem;
import com.project.edusync.hrms.dto.dashboard.GradeDistributionItem;
import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.dto.dashboard.MonthlyPayrollTrendItem;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.HrmsDashboardService;
import com.project.edusync.teacher.repository.ProxyRequestRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrmsDashboardServiceImpl implements HrmsDashboardService {

    private final StaffRepository staffRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final StaffGradeAssignmentRepository staffGradeAssignmentRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;
    private final ProxyRequestRepository proxyRequestRepository;
    private final LateClockInRequestRepository lateClockInRequestRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.HRMS_DASHBOARD_SUMMARY, key = "'summary'")
    public HrmsDashboardSummaryDTO getSummary() {
        LocalDate today = LocalDate.now();

        // ── Static headcounts (3 single queries) ──────────────────────────
        long totalActiveStaffLong = staffRepository.countByIsActiveTrue();
        int totalActiveStaff = toInt(totalActiveStaffLong);

        long mappedLong = staffSalaryMappingRepository.countDistinctStaffWithActiveMappingOnDate(today, LocalDate.of(9999, 12, 31));
        int staffWithSalaryMapping = toInt(mappedLong);
        int staffWithoutSalaryMapping = Math.max(0, totalActiveStaff - staffWithSalaryMapping);

        // ── Today's leave count (1 query) ─────────────────────────────────
        int pendingLeaveApplications = toInt(leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.PENDING));
        int todayOnLeave = toInt(leaveApplicationRepository.countDistinctStaffOnApprovedLeave(today));

        // ── Today's attendance for summary KPIs (2 queries) ───────────────
        int todayPresent = toInt(staffDailyAttendanceRepository.countDistinctPresentStaffByDate(today));
        int todayAbsent = toInt(staffDailyAttendanceRepository.countDistinctAbsentStaffByDate(today));

        // ── Category totals from staff table (3 queries — kept simple) ────
        int totalTeachingStaff = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.TEACHING));
        int totalNonTeachingAdmin = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_ADMIN));
        int totalNonTeachingSupport = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_SUPPORT));

        // ── Category attendance: 2 grouped queries replacing 9 individual ──
        List<CategoryAttendanceItem> categoryAttendance = buildCategoryAttendanceGrouped(today);

        // ── Grade distribution (1 query) ─────────────────────────────────
        List<GradeDistributionItem> gradeDistribution = staffGradeAssignmentRepository.gradeDistribution().stream()
                .map(row -> new GradeDistributionItem(
                        (String) row[0],
                        (String) row[1],
                        toInt(((Number) row[2]).longValue())
                ))
                .toList();

        // ── Payroll trend: 1 grouped query replacing 6 individual ─────────
        YearMonth currentMonth = YearMonth.from(today);
        EnumSet<PayrollRunStatus> payrollStatuses = EnumSet.of(
                PayrollRunStatus.PROCESSED,
                PayrollRunStatus.APPROVED,
                PayrollRunStatus.DISBURSED
        );

        // Two single-month queries for this/last month display
        BigDecimal totalPayrollThisMonth = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                currentMonth.getYear(), currentMonth.getMonthValue(), payrollStatuses);
        BigDecimal totalPayrollLastMonth = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                currentMonth.minusMonths(1).getYear(), currentMonth.minusMonths(1).getMonthValue(), payrollStatuses);

        List<MonthlyPayrollTrendItem> trend = buildLastSixMonthsTrend(currentMonth, payrollStatuses);

        // ── Phase 5: Dashboard intelligence (2 queries) ───────────────────
        int pendingProxyCount = toInt(proxyRequestRepository.countPendingByDate(today));
        int pendingLateClockInCount = toInt(lateClockInRequestRepository.countByStatus(LateClockInStatus.PENDING));

        double staffPresentPercent = totalActiveStaff > 0
                ? Math.min(100.0, Math.round((todayPresent * 100.0 / totalActiveStaff) * 10.0) / 10.0)
                : 0.0;

        return HrmsDashboardSummaryDTO.builder()
                .totalActiveStaff(totalActiveStaff)
                .staffWithSalaryMapping(staffWithSalaryMapping)
                .staffWithoutSalaryMapping(staffWithoutSalaryMapping)
                .totalPayrollThisMonth(nullSafe(totalPayrollThisMonth))
                .totalPayrollLastMonth(nullSafe(totalPayrollLastMonth))
                .pendingLeaveApplications(pendingLeaveApplications)
                .todayPresent(todayPresent)
                .todayAbsent(todayAbsent)
                .todayOnLeave(todayOnLeave)
                .totalTeachingStaff(totalTeachingStaff)
                .totalNonTeachingAdmin(totalNonTeachingAdmin)
                .totalNonTeachingSupport(totalNonTeachingSupport)
                .gradeDistribution(gradeDistribution)
                .payrollTrend(trend)
                .categoryAttendance(categoryAttendance)
                .pendingApprovalRequests(0)
                .pendingProxyCount(pendingProxyCount)
                .pendingLateClockInCount(pendingLateClockInCount)
                .staffPresentPercent(staffPresentPercent)
                .build();
    }

    /**
     * Builds category attendance using 2 grouped queries instead of 9 individual ones.
     * Query 1: attendance records grouped by category (present/absent counts).
     * Query 2: leave records grouped by category (onLeave counts).
     */
    private List<CategoryAttendanceItem> buildCategoryAttendanceGrouped(LocalDate date) {
        // Query 1: present/absent counts per category
        Map<StaffCategory, long[]> attendanceMap = new HashMap<>();
        for (StaffDailyAttendanceRepository.CategoryAttendanceCountProjection p :
                staffDailyAttendanceRepository.countByDateGroupedByCategoryAndAttendanceType(date)) {
            attendanceMap.put(
                    p.getCategory(),
                    new long[]{p.getPresentCount(), p.getAbsentCount()}
            );
        }

        // Query 2: on-leave counts per category
        Map<StaffCategory, Long> leaveMap = new HashMap<>();
        for (LeaveApplicationRepository.CategoryLeaveCountProjection p :
                leaveApplicationRepository.countOnLeaveByDateGroupedByCategory(date)) {
            leaveMap.put(p.getCategory(), p.getOnLeaveCount());
        }

        List<CategoryAttendanceItem> result = new ArrayList<>();
        for (StaffCategory category : List.of(
                StaffCategory.TEACHING,
                StaffCategory.NON_TEACHING_ADMIN,
                StaffCategory.NON_TEACHING_SUPPORT)) {
            long[] counts = attendanceMap.getOrDefault(category, new long[]{0L, 0L});
            long onLeave = leaveMap.getOrDefault(category, 0L);
            result.add(new CategoryAttendanceItem(
                    category,
                    toInt(counts[0]),
                    toInt(counts[1]),
                    toInt(onLeave)
            ));
        }
        return result;
    }

    /**
     * Builds 6-month payroll trend using 1 grouped query instead of 6 individual ones.
     */
    private List<MonthlyPayrollTrendItem> buildLastSixMonthsTrend(YearMonth currentMonth, EnumSet<PayrollRunStatus> statuses) {
        YearMonth startMonth = currentMonth.minusMonths(5);

        // Build a lookup map from the single grouped query result
        Map<String, BigDecimal> payrollByKey = new HashMap<>();
        for (PayrollRunRepository.MonthlyPayrollSumProjection p : payrollRunRepository.sumPayrollGroupedByMonth(
                startMonth.getYear(), startMonth.getMonthValue(),
                currentMonth.getYear(), currentMonth.getMonthValue(),
                statuses)) {
            payrollByKey.put(p.getPayYear() + "-" + p.getPayMonth(), nullSafe(p.getTotalNet()));
        }

        // Build the full 6-month list, filling zero for months with no payroll run
        List<MonthlyPayrollTrendItem> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            String key = month.getYear() + "-" + month.getMonthValue();
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear();
            trend.add(new MonthlyPayrollTrendItem(label, payrollByKey.getOrDefault(key, BigDecimal.ZERO)));
        }
        return trend;
    }

    /**
     * Optimized heatmap: 2 grouped queries instead of N×3 individual queries.
     * Present/absent from attendance table; on-leave from a range query + in-memory expansion.
     */
    @Override
    @Transactional(readOnly = true)
    public AttendanceHeatmapDTO getAttendanceHeatmap(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // Query 1: present/absent counts per day — 1 query replacing 2N
        Map<LocalDate, long[]> attendanceByDate = new HashMap<>();
        for (StaffDailyAttendanceRepository.DailyAttendanceCountProjection p :
                staffDailyAttendanceRepository.heatmapPresentAbsentByDateRange(start, end)) {
            attendanceByDate.put(p.getAttendanceDate(), new long[]{p.getPresentCount(), p.getAbsentCount()});
        }

        // Query 2: all overlapping leave records — 1 query; expand per-day in-memory replacing N queries
        Map<LocalDate, Long> leaveByDate = new HashMap<>();
        for (var la : leaveApplicationRepository.findApprovedLeaveOverlappingRange(start, end)) {
            LocalDate leaveStart = la.getFromDate().isBefore(start) ? start : la.getFromDate();
            LocalDate leaveEnd = la.getToDate().isAfter(end) ? end : la.getToDate();
            LocalDate dayCursor = leaveStart;
            while (!dayCursor.isAfter(leaveEnd)) {
                leaveByDate.merge(dayCursor, 1L, Long::sum);
                dayCursor = dayCursor.plusDays(1);
            }
        }

        // Merge in-memory — fill gaps for days with no records
        List<AttendanceHeatmapDTO.HeatmapDayEntry> days = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            long[] counts = attendanceByDate.getOrDefault(cursor, new long[]{0L, 0L});
            long onLeave = leaveByDate.getOrDefault(cursor, 0L);
            days.add(new AttendanceHeatmapDTO.HeatmapDayEntry(cursor, toInt(counts[0]), toInt(counts[1]), toInt(onLeave)));
            cursor = cursor.plusDays(1);
        }

        return new AttendanceHeatmapDTO(year, month, days);
    }

    private int toInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
