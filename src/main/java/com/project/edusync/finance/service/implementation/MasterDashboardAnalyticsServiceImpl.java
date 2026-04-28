package com.project.edusync.finance.service.implementation;

import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.common.config.CacheNames;
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
