package com.project.edusync.hrms.service.impl;

import com.project.edusync.hrms.dto.dashboard.GradeDistributionItem;
import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.dto.dashboard.MonthlyPayrollTrendItem;
import com.project.edusync.hrms.dto.dashboard.CategoryAttendanceItem;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.model.enums.PayrollRunStatus;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.hrms.service.HrmsDashboardService;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HrmsDashboardServiceImpl implements HrmsDashboardService {

    private final StaffRepository staffRepository;
    private final StaffSalaryMappingRepository staffSalaryMappingRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final StaffGradeAssignmentRepository staffGradeAssignmentRepository;
    private final StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public HrmsDashboardSummaryDTO getSummary() {
        LocalDate today = LocalDate.now();
        long totalActiveStaffLong = staffRepository.countByIsActiveTrue();
        int totalActiveStaff = toInt(totalActiveStaffLong);

        long mappedLong = staffSalaryMappingRepository.countDistinctStaffWithActiveMappingOnDate(today, LocalDate.of(9999, 12, 31));
        int staffWithSalaryMapping = toInt(mappedLong);
        int staffWithoutSalaryMapping = Math.max(0, totalActiveStaff - staffWithSalaryMapping);

        YearMonth currentMonth = YearMonth.from(today);
        YearMonth lastMonth = currentMonth.minusMonths(1);
        EnumSet<PayrollRunStatus> payrollStatuses = EnumSet.of(
                PayrollRunStatus.PROCESSED,
                PayrollRunStatus.APPROVED,
                PayrollRunStatus.DISBURSED
        );

        BigDecimal totalPayrollThisMonth = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                currentMonth.getYear(),
                currentMonth.getMonthValue(),
                payrollStatuses
        );
        BigDecimal totalPayrollLastMonth = payrollRunRepository.sumTotalNetByMonthAndStatuses(
                lastMonth.getYear(),
                lastMonth.getMonthValue(),
                payrollStatuses
        );

        int pendingLeaveApplications = toInt(leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.PENDING));
        int todayOnLeave = toInt(leaveApplicationRepository.countDistinctStaffOnApprovedLeave(today));

        int todayPresent = toInt(staffDailyAttendanceRepository.countDistinctPresentStaffByDate(today));
        int todayAbsent = toInt(staffDailyAttendanceRepository.countDistinctAbsentStaffByDate(today));

        int totalTeachingStaff = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.TEACHING));
        int totalNonTeachingAdmin = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_ADMIN));
        int totalNonTeachingSupport = toInt(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_SUPPORT));

        List<CategoryAttendanceItem> categoryAttendance = List.of(
                buildCategoryAttendance(today, StaffCategory.TEACHING),
                buildCategoryAttendance(today, StaffCategory.NON_TEACHING_ADMIN),
                buildCategoryAttendance(today, StaffCategory.NON_TEACHING_SUPPORT)
        );

        List<GradeDistributionItem> gradeDistribution = staffGradeAssignmentRepository.gradeDistribution().stream()
                .map(row -> new GradeDistributionItem(
                        (String) row[0],
                        (String) row[1],
                        toInt(((Number) row[2]).longValue())
                ))
                .toList();

        List<MonthlyPayrollTrendItem> trend = buildLastSixMonthsTrend(currentMonth, payrollStatuses);

        return new HrmsDashboardSummaryDTO(
                totalActiveStaff,
                staffWithSalaryMapping,
                staffWithoutSalaryMapping,
                nullSafe(totalPayrollThisMonth),
                nullSafe(totalPayrollLastMonth),
                pendingLeaveApplications,
                todayPresent,
                todayAbsent,
                todayOnLeave,
                totalTeachingStaff,
                totalNonTeachingAdmin,
                totalNonTeachingSupport,
                gradeDistribution,
                trend,
                categoryAttendance
        );
    }

    private CategoryAttendanceItem buildCategoryAttendance(LocalDate date, StaffCategory category) {
        int present = toInt(staffDailyAttendanceRepository.countDistinctPresentStaffByDateAndCategory(date, category));
        int absent = toInt(staffDailyAttendanceRepository.countDistinctAbsentStaffByDateAndCategory(date, category));
        int onLeave = toInt(leaveApplicationRepository.countDistinctStaffOnApprovedLeaveByCategoryAndDate(category, date));
        return new CategoryAttendanceItem(category, present, absent, onLeave);
    }

    private List<MonthlyPayrollTrendItem> buildLastSixMonthsTrend(YearMonth currentMonth, EnumSet<PayrollRunStatus> statuses) {
        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(i -> currentMonth.minusMonths(5L - i))
                .map(month -> new MonthlyPayrollTrendItem(
                        month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear(),
                        nullSafe(payrollRunRepository.sumTotalNetByMonthAndStatuses(month.getYear(), month.getMonthValue(), statuses))
                ))
                .toList();
    }

    private int toInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }


    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}



