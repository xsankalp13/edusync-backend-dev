package com.project.edusync.hrms.service.impl;

import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.uis.model.enums.StaffCategory;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrmsDashboardServiceImplTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffSalaryMappingRepository staffSalaryMappingRepository;
    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private StaffGradeAssignmentRepository staffGradeAssignmentRepository;
    @Mock
    private StaffDailyAttendanceRepository staffDailyAttendanceRepository;

    @InjectMocks
    private HrmsDashboardServiceImpl service;

    @Test
    void getSummaryBuildsDashboardMetrics() {
        LocalDate today = LocalDate.now();

        when(staffRepository.countByIsActiveTrue()).thenReturn(10L);
        when(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.TEACHING)).thenReturn(4L);
        when(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_ADMIN)).thenReturn(3L);
        when(staffRepository.countByIsActiveTrueAndCategory(StaffCategory.NON_TEACHING_SUPPORT)).thenReturn(3L);
        when(staffSalaryMappingRepository.countDistinctStaffWithActiveMappingOnDate(today, LocalDate.of(9999, 12, 31))).thenReturn(7L);
        when(leaveApplicationRepository.countByActiveTrueAndStatus(LeaveApplicationStatus.PENDING)).thenReturn(3L);
        when(leaveApplicationRepository.countDistinctStaffOnApprovedLeave(today)).thenReturn(2L);
        when(leaveApplicationRepository.countDistinctStaffOnApprovedLeaveByCategoryAndDate(StaffCategory.TEACHING, today)).thenReturn(1L);
        when(leaveApplicationRepository.countDistinctStaffOnApprovedLeaveByCategoryAndDate(StaffCategory.NON_TEACHING_ADMIN, today)).thenReturn(1L);
        when(leaveApplicationRepository.countDistinctStaffOnApprovedLeaveByCategoryAndDate(StaffCategory.NON_TEACHING_SUPPORT, today)).thenReturn(0L);
        when(staffDailyAttendanceRepository.countDistinctPresentStaffByDate(today)).thenReturn(6L);
        when(staffDailyAttendanceRepository.countDistinctAbsentStaffByDate(today)).thenReturn(1L);
        when(staffDailyAttendanceRepository.countDistinctPresentStaffByDateAndCategory(today, StaffCategory.TEACHING)).thenReturn(3L);
        when(staffDailyAttendanceRepository.countDistinctPresentStaffByDateAndCategory(today, StaffCategory.NON_TEACHING_ADMIN)).thenReturn(2L);
        when(staffDailyAttendanceRepository.countDistinctPresentStaffByDateAndCategory(today, StaffCategory.NON_TEACHING_SUPPORT)).thenReturn(1L);
        when(staffDailyAttendanceRepository.countDistinctAbsentStaffByDateAndCategory(today, StaffCategory.TEACHING)).thenReturn(1L);
        when(staffDailyAttendanceRepository.countDistinctAbsentStaffByDateAndCategory(today, StaffCategory.NON_TEACHING_ADMIN)).thenReturn(0L);
        when(staffDailyAttendanceRepository.countDistinctAbsentStaffByDateAndCategory(today, StaffCategory.NON_TEACHING_SUPPORT)).thenReturn(0L);

        when(payrollRunRepository.sumTotalNetByMonthAndStatuses(any(), any(), any()))
                .thenReturn(new BigDecimal("120000.00"));

        when(staffGradeAssignmentRepository.gradeDistribution()).thenReturn(List.of(
                new Object[]{"PRT", "Primary Teacher", 4L},
                new Object[]{"TGT", "Trained Graduate Teacher", 3L}
        ));

        HrmsDashboardSummaryDTO result = service.getSummary();

        assertEquals(10, result.totalActiveStaff());
        assertEquals(7, result.staffWithSalaryMapping());
        assertEquals(3, result.staffWithoutSalaryMapping());
        assertEquals(3, result.pendingLeaveApplications());
        assertEquals(2, result.todayOnLeave());
        assertEquals(6, result.todayPresent());
        assertEquals(1, result.todayAbsent());
        assertEquals(2, result.gradeDistribution().size());
        assertEquals(6, result.payrollTrend().size());
        assertEquals(4, result.totalTeachingStaff());
        assertEquals(3, result.totalNonTeachingAdmin());
        assertEquals(3, result.totalNonTeachingSupport());
        assertEquals(3, result.categoryAttendance().size());
    }
}



