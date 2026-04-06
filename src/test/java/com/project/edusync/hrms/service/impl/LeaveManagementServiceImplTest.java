package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveReviewDTO;
import com.project.edusync.hrms.model.entity.LeaveApplication;
import com.project.edusync.hrms.model.entity.LeaveBalance;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.repository.LeaveApplicationRepository;
import com.project.edusync.hrms.repository.LeaveBalanceRepository;
import com.project.edusync.hrms.repository.LeaveTypeConfigRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveManagementServiceImplTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private LeaveTypeConfigRepository leaveTypeConfigRepository;
    @Mock
    private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private LeaveManagementServiceImpl service;

    @Test
    void applyThrowsConflictWhenOverlapExists() {
        Staff staff = buildStaff(101L, "Asha", "Verma");
        LeaveTypeConfig leaveType = buildLeaveType(11L, "CL", 12);

        when(authUtil.getCurrentUserId()).thenReturn(2001L);
        when(staffRepository.findByUserProfile_User_Id(2001L)).thenReturn(Optional.of(staff));
        when(leaveTypeConfigRepository.findById(11L)).thenReturn(Optional.of(leaveType));
        when(leaveApplicationRepository.existsOverlapping(
                101L,
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 16),
                Set.of(LeaveApplicationStatus.PENDING, LeaveApplicationStatus.APPROVED)
        )).thenReturn(true);

        LeaveApplicationCreateDTO dto = new LeaveApplicationCreateDTO(
                "11",
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 16),
                false,
                null,
                "Personal work",
                null
        );

        assertThrows(EdusyncException.class, () -> service.applyForCurrentStaff(dto));
    }

    @Test
    void applyRejectsHalfDayAcrossMultipleDates() {
        LeaveApplicationCreateDTO dto = new LeaveApplicationCreateDTO(
                "11",
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 15),
                true,
                com.project.edusync.hrms.model.enums.HalfDayType.FIRST_HALF,
                "Medical",
                null
        );

        assertThrows(EdusyncException.class, () -> service.applyForCurrentStaff(dto));
    }

    @Test
    void applyResolvesLeaveTypeByUuidReference() {
        Staff staff = buildStaff(101L, "Asha", "Verma");
        LeaveTypeConfig leaveType = buildLeaveType(11L, "CL", 12);
        UUID leaveTypeUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        leaveType.setUuid(leaveTypeUuid);

        when(authUtil.getCurrentUserId()).thenReturn(2001L);
        when(staffRepository.findByUserProfile_User_Id(2001L)).thenReturn(Optional.of(staff));
        when(leaveTypeConfigRepository.findByUuid(leaveTypeUuid)).thenReturn(Optional.of(leaveType));
        when(leaveApplicationRepository.existsOverlapping(
                101L,
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                Set.of(LeaveApplicationStatus.PENDING, LeaveApplicationStatus.APPROVED)
        )).thenReturn(false);
        when(leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(101L, 11L, "2026-2027"))
                .thenReturn(Optional.of(buildBalance(staff, leaveType)));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> {
            LeaveApplication application = invocation.getArgument(0);
            application.setId(900L);
            application.setActive(true);
            application.setAppliedOn(java.time.LocalDateTime.now());
            return application;
        });

        LeaveApplicationCreateDTO dto = new LeaveApplicationCreateDTO(
                leaveTypeUuid.toString(),
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                false,
                null,
                "Personal work",
                null
        );

        LeaveApplicationResponseDTO response = service.applyForCurrentStaff(dto);

        assertEquals(LeaveApplicationStatus.PENDING, response.status());
        verify(leaveTypeConfigRepository).findByUuid(leaveTypeUuid);
        verify(leaveTypeConfigRepository, never()).findById(11L);
    }

    @Test
    void approveDeductsLeaveBalance() {
        Staff staff = buildStaff(101L, "Ravi", "Sharma");
        Staff approver = buildStaff(102L, "Admin", "User");
        LeaveTypeConfig leaveType = buildLeaveType(12L, "CL", 12);

        LeaveApplication application = new LeaveApplication();
        application.setId(501L);
        application.setActive(true);
        application.setStaff(staff);
        application.setLeaveType(leaveType);
        application.setFromDate(LocalDate.of(2026, 4, 10));
        application.setToDate(LocalDate.of(2026, 4, 10));
        application.setTotalDays(new BigDecimal("1.00"));
        application.setStatus(LeaveApplicationStatus.PENDING);
        application.setAppliedOn(java.time.LocalDateTime.now());
        application.setReason("Urgent work");

        LeaveBalance balance = new LeaveBalance();
        balance.setStaff(staff);
        balance.setLeaveType(leaveType);
        balance.setAcademicYear("2026-2027");
        balance.setTotalQuota(new BigDecimal("12.00"));
        balance.setUsed(new BigDecimal("2.00"));

        when(leaveApplicationRepository.findById(501L)).thenReturn(Optional.of(application));
        when(leaveApplicationRepository.existsOverlappingExcludingId(any(), any(), any(), any(), any())).thenReturn(false);
        when(staffRepository.findByUserProfile_User_Id(9001L)).thenReturn(Optional.of(approver));
        when(leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(101L, 12L, "2026-2027"))
                .thenReturn(Optional.of(balance));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplicationResponseDTO response = service.approve(501L, 9001L, new LeaveReviewDTO("Approved"));

        assertEquals(LeaveApplicationStatus.APPROVED, response.status());
        assertEquals(new BigDecimal("3.00"), balance.getUsed());
    }

    @Test
    void listApplicationsNormalizesLeaveTypeCodeToUppercaseAndTrimmed() {
        LeaveApplication application = new LeaveApplication();
        application.setId(601L);
        application.setActive(true);
        application.setStatus(LeaveApplicationStatus.PENDING);
        application.setFromDate(LocalDate.of(2026, 4, 10));
        application.setToDate(LocalDate.of(2026, 4, 10));
        application.setTotalDays(new BigDecimal("1.00"));
        application.setReason("Personal work");
        application.setStaff(buildStaff(301L, "Nina", "Kapoor"));
        application.setLeaveType(buildLeaveType(41L, "CL", 12));

        Page<LeaveApplication> page = new PageImpl<>(List.of(application));
        when(leaveApplicationRepository.search(anyLong(), any(), any(), any(), any(), any())).thenReturn(page);

        service.listApplications(
                9001L,
                true,
                301L,
                LeaveApplicationStatus.PENDING,
                "  cl  ",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                PageRequest.of(0, 10)
        );

        verify(leaveApplicationRepository).search(
                eq(301L),
                eq(LeaveApplicationStatus.PENDING),
                eq("CL"),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void listApplicationsTreatsBlankLeaveTypeCodeAsNull() {
        when(leaveApplicationRepository.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listApplications(
                9001L,
                true,
                null,
                null,
                "   ",
                null,
                null,
                PageRequest.of(0, 20)
        );

        verify(leaveApplicationRepository).search(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(PageRequest.of(0, 20))
        );
    }

    private Staff buildStaff(Long id, String firstName, String lastName) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setActive(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        staff.setUserProfile(profile);
        return staff;
    }

    private LeaveTypeConfig buildLeaveType(Long id, String code, int annualQuota) {
        LeaveTypeConfig leaveType = new LeaveTypeConfig();
        leaveType.setId(id);
        leaveType.setActive(true);
        leaveType.setLeaveCode(code);
        leaveType.setDisplayName(code + " Leave");
        leaveType.setAnnualQuota(annualQuota);
        leaveType.setMinDaysBeforeApply(0);
        leaveType.setRequiresDocument(false);
        leaveType.setMaxConsecutiveDays(30);
        leaveType.setCarryForwardAllowed(false);
        leaveType.setMaxCarryForward(0);
        return leaveType;
    }

    private LeaveBalance buildBalance(Staff staff, LeaveTypeConfig leaveType) {
        LeaveBalance balance = new LeaveBalance();
        balance.setStaff(staff);
        balance.setLeaveType(leaveType);
        balance.setAcademicYear("2026-2027");
        balance.setTotalQuota(new BigDecimal("12.00"));
        balance.setUsed(new BigDecimal("1.00"));
        return balance;
    }
}
