package com.project.edusync.finance.service.implementation;

import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.common.config.CacheNames;
import com.project.edusync.finance.dto.dashboard.DashboardForecastDTO;
import com.project.edusync.finance.dto.dashboard.DashboardKpiTrendsDTO;
import com.project.edusync.finance.dto.dashboard.MasterAnalyticsResponseDTO;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.finance.repository.PaymentRepository;
import com.project.edusync.finance.service.MasterDashboardAnalyticsService;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MasterDashboardAnalyticsServiceImpl implements MasterDashboardAnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private static final Pattern CLASS_NUMBER_PATTERN = Pattern.compile("(\\d{1,2})");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MASTER_DASHBOARD_ANALYTICS, key = "'master'")
    public MasterAnalyticsResponseDTO getMasterAnalytics() {
        return MasterAnalyticsResponseDTO.builder()
                .financePayrollTrend(buildFinancePayrollTrend())
                .attendanceTrend(buildAttendanceTrend())
                .demographics(buildDemographics())
                .build();
    }

    /**
     * Returns KPI trend comparisons: current MTD vs prior month MTD.
     * Revenue, Outstanding, Payroll — each with real delta %.
     * Result is intentionally NOT cached: MTD is a moving window.
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardKpiTrendsDTO getKpiTrends() {
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();

        // Current MTD: 1st of this month → today
        LocalDate currentStart = today.withDayOfMonth(1);

        // Prior month MTD: same span (1st → same day-of-month, capped to last day)
        YearMonth priorYm = YearMonth.of(today.getYear(), today.getMonthValue()).minusMonths(1);
        LocalDate priorStart = priorYm.atDay(1);
        LocalDate priorEnd = priorYm.atDay(Math.min(dayOfMonth, priorYm.lengthOfMonth()));

        // ── Revenue (collected payments) ────────────────────────────
        BigDecimal revenueMtd = paymentRepository.sumCollectedByDateRange(currentStart.atStartOfDay(), today.atTime(23, 59, 59));
        BigDecimal revenuePriorMtd = paymentRepository.sumCollectedByDateRange(priorStart.atStartOfDay(), priorEnd.atTime(23, 59, 59));

        // ── Outstanding (invoices issued this month, unpaid balance) ─
        BigDecimal expectedMtd = invoiceRepository.sumExpectedByDateRange(currentStart, today);
        BigDecimal collectedMtd = revenueMtd;
        BigDecimal outstandingMtd = expectedMtd.subtract(collectedMtd).max(BigDecimal.ZERO);

        BigDecimal expectedPrior = invoiceRepository.sumExpectedByDateRange(priorStart, priorEnd);
        BigDecimal collectedPrior = revenuePriorMtd;
        BigDecimal outstandingPriorMtd = expectedPrior.subtract(collectedPrior).max(BigDecimal.ZERO);

        // ── Payroll outflow ──────────────────────────────────────────
        EnumSet<PayrollRunStatus> disbursedStatuses = EnumSet.of(
                PayrollRunStatus.PROCESSED, PayrollRunStatus.APPROVED, PayrollRunStatus.DISBURSED);
        BigDecimal payrollMtd = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                today.getYear(), today.getMonthValue(), disbursedStatuses);
        BigDecimal payrollPriorMtd = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                priorYm.getYear(), priorYm.getMonthValue(), disbursedStatuses);

        // ── Pending invoices ─────────────────────────────────────────
        long pendingCount = invoiceRepository.countPendingInvoices();

        return DashboardKpiTrendsDTO.builder()
                .revenueMtd(revenueMtd)
                .revenuePriorMtd(revenuePriorMtd)
                .revenueDeltaPct(pctChange(revenuePriorMtd, revenueMtd))
                .outstandingMtd(outstandingMtd)
                .outstandingPriorMtd(outstandingPriorMtd)
                .outstandingDeltaPct(pctChange(outstandingPriorMtd, outstandingMtd))
                .payrollMtd(payrollMtd)
                .payrollPriorMtd(payrollPriorMtd)
                .payrollDeltaPct(pctChange(payrollPriorMtd, payrollMtd))
                .pendingInvoiceCount(pendingCount)
                .build();
    }

    /**
     * Predictive intelligence snapshot for the admin dashboard.
     * Computes three forecast signals without caching (all moving windows):
     *   1. Revenue EOM projection  — linear extrapolation of MTD daily rate.
     *   2. Staff attendance trend  — 7-day rolling comparison (last-3 vs first-4 avg).
     *   3. Outstanding balance risk — month-over-month growth rate classification.
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardForecastDTO getForecast() {
        LocalDate today = LocalDate.now();
        int daysElapsed = today.getDayOfMonth();
        int daysInMonth = today.lengthOfMonth();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(daysInMonth);

        // ── Revenue EOM forecast ─────────────────────────────────────────
        BigDecimal collectedMtd = paymentRepository.sumCollectedByDateRange(
                monthStart.atStartOfDay(), today.atTime(23, 59, 59));
        BigDecimal monthTarget = invoiceRepository.sumExpectedByDateRange(monthStart, monthEnd);

        double dailyRate = daysElapsed > 0 ? collectedMtd.doubleValue() / daysElapsed : 0.0;
        double eomForecastVal = dailyRate * daysInMonth;
        double trajectoryPct = monthTarget.compareTo(BigDecimal.ZERO) > 0
                ? (eomForecastVal / monthTarget.doubleValue()) * 100.0
                : 100.0;
        String revenueTrajectory = trajectoryPct >= 90.0 ? "ON_TRACK"
                : trajectoryPct >= 70.0 ? "AT_RISK"
                : "CRITICAL";

        // ── Staff attendance 7-day trend ──────────────────────────────────
        LocalDate sevenDaysAgo = today.minusDays(6);
        long totalStaff = staffRepository.countByIsActiveTrue();

        Map<LocalDate, Long> presentByDay = new HashMap<>();
        staffDailyAttendanceRepository.countPresentStaffByDateRange(sevenDaysAgo, today)
                .forEach(p -> presentByDay.put(p.getAttendanceDate(), p.getPresentCount()));

        List<Double> pctList = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long present = presentByDay.getOrDefault(d, 0L);
            pctList.add(totalStaff > 0 ? (present * 100.0) / totalStaff : 0.0);
        }

        double currentAttPct = pctList.get(pctList.size() - 1);
        double firstAvg = pctList.subList(0, 4).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double lastAvg  = pctList.subList(4, 7).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double slope = round1(lastAvg - firstAvg);
        String attendanceTrend = slope > 2.0 ? "IMPROVING" : slope < -2.0 ? "DECLINING" : "STABLE";

        // ── Outstanding risk (MoM growth) ────────────────────────────────
        BigDecimal expectedMtd = invoiceRepository.sumExpectedByDateRange(monthStart, today);
        BigDecimal outstandingMtd = expectedMtd.subtract(collectedMtd).max(BigDecimal.ZERO);

        YearMonth priorYm = YearMonth.of(today.getYear(), today.getMonthValue()).minusMonths(1);
        LocalDate priorStart = priorYm.atDay(1);
        LocalDate priorEnd = priorYm.atDay(Math.min(daysElapsed, priorYm.lengthOfMonth()));
        BigDecimal collectedPrior = paymentRepository.sumCollectedByDateRange(
                priorStart.atStartOfDay(), priorEnd.atTime(23, 59, 59));
        BigDecimal expectedPrior = invoiceRepository.sumExpectedByDateRange(priorStart, priorEnd);
        BigDecimal outstandingPrior = expectedPrior.subtract(collectedPrior).max(BigDecimal.ZERO);

        double outstandingGrowthRate = round1(pctChange(outstandingPrior, outstandingMtd));
        String outstandingRisk = outstandingGrowthRate > 20.0 ? "HIGH"
                : outstandingGrowthRate > 5.0 ? "MEDIUM"
                : "LOW";

        return DashboardForecastDTO.builder()
                .revenueEomForecast(BigDecimal.valueOf(eomForecastVal).setScale(0, RoundingMode.HALF_UP))
                .revenueMonthTarget(monthTarget)
                .revenueTrajectoryPct(round1(trajectoryPct))
                .revenueTrajectory(revenueTrajectory)
                .attendanceTrend(attendanceTrend)
                .attendanceTrendSlope(slope)
                .currentStaffAttendancePct(round1(currentAttPct))
                .outstandingRisk(outstandingRisk)
                .outstandingGrowthRate(outstandingGrowthRate)
                .build();
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Computes percentage change: (current - prior) / prior * 100, returns 0 if prior == 0. */
    private double pctChange(BigDecimal prior, BigDecimal current) {
        if (prior == null || prior.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return current.subtract(prior)
                .divide(prior, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Builds 6-month finance/payroll trend using 3 grouped queries instead of 18 individual ones.
     * Before: 3 queries × 6 months = 18 queries.
     * After: 3 grouped range queries = 3 queries.
     */
    private List<MasterAnalyticsResponseDTO.FinancePayrollPoint> buildFinancePayrollTrend() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(5);

        LocalDate rangeStart = startMonth.atDay(1);
        LocalDate rangeEnd = currentMonth.atEndOfMonth();

        // 3 grouped range queries
        Map<String, BigDecimal> expectedByKey = new HashMap<>();
        for (InvoiceRepository.MonthlyInvoiceSumProjection p :
                invoiceRepository.sumExpectedGroupedByMonth(rangeStart, rangeEnd)) {
            expectedByKey.put(p.getInvoiceYear() + "-" + p.getInvoiceMonth(), nullSafe(p.getExpectedTotal()));
        }

        Map<String, BigDecimal> collectedByKey = new HashMap<>();
        for (PaymentRepository.MonthlyPaymentSumProjection p :
                paymentRepository.sumCollectedGroupedByMonth(
                        rangeStart.atStartOfDay(),
                        rangeEnd.atTime(23, 59, 59))) {
            collectedByKey.put(p.getPaymentYear() + "-" + p.getPaymentMonth(), nullSafe(p.getCollectedTotal()));
        }

        EnumSet<PayrollRunStatus> payrollStatuses = EnumSet.of(
                PayrollRunStatus.PROCESSED, PayrollRunStatus.APPROVED, PayrollRunStatus.DISBURSED);
        Map<String, BigDecimal> payrollByKey = new HashMap<>();
        for (PayrollRunRepository.MonthlyPayrollSumProjection p :
                payrollRunRepository.sumPayrollGroupedByMonth(
                        startMonth.getYear(), startMonth.getMonthValue(),
                        currentMonth.getYear(), currentMonth.getMonthValue(),
                        payrollStatuses)) {
            payrollByKey.put(p.getPayYear() + "-" + p.getPayMonth(), nullSafe(p.getTotalNet()));
        }

        // Assemble 6 points in-memory
        List<MasterAnalyticsResponseDTO.FinancePayrollPoint> points = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth target = currentMonth.minusMonths(i);
            String key = target.getYear() + "-" + target.getMonthValue();
            points.add(MasterAnalyticsResponseDTO.FinancePayrollPoint.builder()
                    .month(target.format(MONTH_LABEL))
                    .expected(expectedByKey.getOrDefault(key, BigDecimal.ZERO))
                    .collected(collectedByKey.getOrDefault(key, BigDecimal.ZERO))
                    .payroll(payrollByKey.getOrDefault(key, BigDecimal.ZERO))
                    .build());
        }
        return points;
    }

    /**
     * Builds 14-day attendance trend using 4 queries instead of 30.
     * Before: 2 static counts + (2 per-day queries × 14 days) = 30 queries.
     * After: 2 static counts + 2 grouped range queries = 4 queries.
     */
    private List<MasterAnalyticsResponseDTO.AttendancePoint> buildAttendanceTrend() {
        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusDays(13);

        // 2 static headcount queries (already efficient)
        long totalActiveStudents = studentRepository.countByIsActiveTrue();
        long totalActiveStaff = staffRepository.countByIsActiveTrue();

        // 1 grouped query replacing 14 student per-day queries
        Map<LocalDate, Long> presentStudentsByDate = new HashMap<>();
        for (StudentDailyAttendanceRepository.DailyStudentCountProjection p :
                studentDailyAttendanceRepository.countPresentStudentsByDateRange(rangeStart, today)) {
            presentStudentsByDate.put(p.getAttendanceDate(), p.getPresentCount());
        }

        // 1 grouped query replacing 14 staff per-day queries
        Map<LocalDate, Long> presentStaffByDate = new HashMap<>();
        for (StaffDailyAttendanceRepository.DailyAttendanceCountProjection p :
                staffDailyAttendanceRepository.countPresentStaffByDateRange(rangeStart, today)) {
            presentStaffByDate.put(p.getAttendanceDate(), p.getPresentCount());
        }

        // Assemble 14 points in-memory
        List<MasterAnalyticsResponseDTO.AttendancePoint> points = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long presentStudents = presentStudentsByDate.getOrDefault(date, 0L);
            long presentStaff = presentStaffByDate.getOrDefault(date, 0L);
            points.add(MasterAnalyticsResponseDTO.AttendancePoint.builder()
                    .day(date.format(DAY_LABEL))
                    .student(toPercent(presentStudents, totalActiveStudents))
                    .staff(toPercent(presentStaff, totalActiveStaff))
                    .build());
        }
        return points;
    }

    /**
     * Builds demographics using 2 queries instead of 4.
     * Student class grouping kept as-is (already efficient).
     * Staff category counts collapsed into 1 grouped query.
     */
    private List<MasterAnalyticsResponseDTO.DemographicPoint> buildDemographics() {
        long primaryStudents = 0;
        long secondaryStudents = 0;

        for (Object[] row : studentRepository.countActiveStudentsGroupedByClassName()) {
            String className = row[0] == null ? "" : row[0].toString();
            String normalized = className.toLowerCase(Locale.ENGLISH);
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            Integer classNumber = extractClassNumber(className);

            if (classNumber != null && classNumber >= 1 && classNumber <= 5) {
                primaryStudents += count;
            } else if (classNumber != null && classNumber >= 6 && classNumber <= 12) {
                secondaryStudents += count;
            } else if (normalized.contains("primary")) {
                primaryStudents += count;
            } else if (normalized.contains("secondary")) {
                secondaryStudents += count;
            }
        }

        // 1 grouped query replacing 3 individual category count queries
        Map<StaffCategory, Long> staffByCategory = new HashMap<>();
        for (StaffRepository.StaffCategoryCountProjection p : staffRepository.countActiveStaffGroupedByCategory()) {
            staffByCategory.put(p.getCategory(), p.getCount());
        }

        long teachingStaff = staffByCategory.getOrDefault(StaffCategory.TEACHING, 0L);
        long supportStaff = staffByCategory.getOrDefault(StaffCategory.NON_TEACHING_ADMIN, 0L)
                + staffByCategory.getOrDefault(StaffCategory.NON_TEACHING_SUPPORT, 0L);

        return List.of(
                MasterAnalyticsResponseDTO.DemographicPoint.builder()
                        .name("Students Primary")
                        .value(primaryStudents)
                        .color("#3b82f6")
                        .build(),
                MasterAnalyticsResponseDTO.DemographicPoint.builder()
                        .name("Students Secondary")
                        .value(secondaryStudents)
                        .color("#60a5fa")
                        .build(),
                MasterAnalyticsResponseDTO.DemographicPoint.builder()
                        .name("Teaching Staff")
                        .value(teachingStaff)
                        .color("#8b5cf6")
                        .build(),
                MasterAnalyticsResponseDTO.DemographicPoint.builder()
                        .name("Support Staff")
                        .value(supportStaff)
                        .color("#c4b5fd")
                        .build()
        );
    }

    private Integer extractClassNumber(String className) {
        Matcher matcher = CLASS_NUMBER_PATTERN.matcher(className == null ? "" : className);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double toPercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        double raw = (numerator * 100.0) / denominator;
        double clamped = Math.max(0.0, Math.min(100.0, raw));
        return BigDecimal.valueOf(clamped).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
