package com.project.edusync.hrms.service.impl;

import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.PayrollRunRepository;
import com.project.edusync.hrms.repository.StaffGradeAssignmentRepository;
import com.project.edusync.hrms.repository.StaffSalaryMappingRepository;
import com.project.edusync.ams.model.enums.LateClockInStatus;
import com.project.edusync.ams.model.repository.LateClockInRequestRepository;
import com.project.edusync.ams.model.repository.StaffDailyAttendanceRepository;
import com.project.edusync.teacher.repository.ProxyRequestRepository;
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
    @Mock
    private ProxyRequestRepository proxyRequestRepository;
    @Mock
    private LateClockInRequestRepository lateClockInRequestRepository;

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
        when(staffDailyAttendanceRepository.countDistinctPresentStaffByDate(today)).thenReturn(6L);
        when(staffDailyAttendanceRepository.countDistinctAbsentStaffByDate(today)).thenReturn(1L);

        when(payrollRunRepository.sumTotalNetByMonthAndStatuses(any(), any(), any()))
                .thenReturn(new BigDecimal("120000.00"));

        when(staffGradeAssignmentRepository.gradeDistribution()).thenReturn(List.of(
                new Object[]{"PRT", "Primary Teacher", 4L},
                new Object[]{"TGT", "Trained Graduate Teacher", 3L}
        ));
        when(proxyRequestRepository.countPendingByDate(today)).thenReturn(0L);
        when(lateClockInRequestRepository.countByStatus(LateClockInStatus.PENDING)).thenReturn(0L);

        HrmsDashboardSummaryDTO result = service.getSummary();

        assertEquals(10, result.getTotalActiveStaff());
        assertEquals(7, result.getStaffWithSalaryMapping());
        assertEquals(3, result.getStaffWithoutSalaryMapping());
        assertEquals(3, result.getPendingLeaveApplications());
        assertEquals(2, result.getTodayOnLeave());
        assertEquals(6, result.getTodayPresent());
        assertEquals(1, result.getTodayAbsent());
        assertEquals(2, result.getGradeDistribution().size());
        assertEquals(6, result.getPayrollTrend().size());
        assertEquals(4, result.getTotalTeachingStaff());
        assertEquals(3, result.getTotalNonTeachingAdmin());
        assertEquals(3, result.getTotalNonTeachingSupport());
        assertEquals(3, result.getCategoryAttendance().size());
    }
}



