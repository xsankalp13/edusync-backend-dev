package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveReviewDTO;
import com.project.edusync.hrms.model.entity.LeaveApplication;
import com.project.edusync.hrms.model.entity.LeaveBalance;
import com.project.edusync.hrms.model.entity.LeaveTypeConfig;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveManagementLifecycleFlowTest {

    @Mock private LeaveApplicationRepository leaveApplicationRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private LeaveTypeConfigRepository leaveTypeConfigRepository;
    @Mock private AcademicCalendarEventRepository academicCalendarEventRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private AuthUtil authUtil;

    @InjectMocks
    private LeaveManagementServiceImpl service;

    @Test
    void applyThenApproveDeductsBalanceInSingleFlow() {
        Staff applicant = buildStaff(101L, "Ravi", "Kumar");
        Staff approver = buildStaff(102L, "Admin", "User");
        LeaveTypeConfig leaveType = buildLeaveType(11L, "CL", 12);

        LeaveBalance balance = new LeaveBalance();
        balance.setStaff(applicant);
        balance.setLeaveType(leaveType);
        balance.setAcademicYear("2026-2027");
        balance.setTotalQuota(new BigDecimal("12.00"));
        balance.setUsed(BigDecimal.ZERO);

        when(authUtil.getCurrentUserId()).thenReturn(2001L, 9001L);
        when(staffRepository.findByUserProfile_User_Id(2001L)).thenReturn(Optional.of(applicant));
        when(staffRepository.findByUserProfile_User_Id(9001L)).thenReturn(Optional.of(approver));
        when(leaveTypeConfigRepository.findById(11L)).thenReturn(Optional.of(leaveType));
        when(leaveApplicationRepository.existsOverlapping(
                101L,
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                Set.of(LeaveApplicationStatus.PENDING, LeaveApplicationStatus.APPROVED)
        )).thenReturn(false);
        when(leaveBalanceRepository.findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(101L, 11L, "2026-2027"))
                .thenReturn(Optional.of(balance));

        when(leaveApplicationRepository.save(any(LeaveApplication.class))).thenAnswer(invocation -> {
            LeaveApplication application = invocation.getArgument(0);
            if (application.getId() == null) {
                application.setId(555L);
                application.setActive(true);
                application.setAppliedOn(LocalDateTime.now());
            }
            return application;
        });
        when(leaveApplicationRepository.findById(555L)).thenAnswer(invocation -> {
            LeaveApplication application = new LeaveApplication();
            application.setId(555L);
            application.setActive(true);
            application.setStaff(applicant);
            application.setLeaveType(leaveType);
            application.setFromDate(LocalDate.of(2026, 4, 14));
            application.setToDate(LocalDate.of(2026, 4, 14));
            application.setTotalDays(new BigDecimal("1.00"));
            application.setStatus(LeaveApplicationStatus.PENDING);
            application.setAppliedOn(LocalDateTime.now());
            application.setReason("Personal work");
            return Optional.of(application);
        });
        when(leaveApplicationRepository.existsOverlappingExcludingId(any(), any(), any(), any(), any())).thenReturn(false);
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApplicationCreateDTO request = new LeaveApplicationCreateDTO(
                "11",
                LocalDate.of(2026, 4, 14),
                LocalDate.of(2026, 4, 14),
                false,
                null,
                "Personal work",
                null
        );

        LeaveApplicationResponseDTO applied = service.applyForCurrentStaff(request);
        LeaveApplicationResponseDTO approved = service.approve(applied.applicationId(), 9001L, new LeaveReviewDTO("Approved"));

        assertEquals(LeaveApplicationStatus.PENDING, applied.status());
        assertEquals(LeaveApplicationStatus.APPROVED, approved.status());
        assertEquals(new BigDecimal("1.00"), balance.getUsed());
    }

    @Test
    void approveRejectsSelfApprovalByApplicantUser() {
        Staff applicant = buildStaff(201L, "Priya", "Principal");
        com.project.edusync.iam.model.entity.User applicantUser = new com.project.edusync.iam.model.entity.User();
        applicantUser.setId(9001L);
        applicant.setUser(applicantUser);

        LeaveTypeConfig leaveType = buildLeaveType(21L, "CL", 12);

        LeaveApplication application = new LeaveApplication();
        application.setId(777L);
        application.setActive(true);
        application.setStaff(applicant);
        application.setLeaveType(leaveType);
        application.setFromDate(LocalDate.of(2026, 4, 20));
        application.setToDate(LocalDate.of(2026, 4, 20));
        application.setTotalDays(new BigDecimal("1.00"));
        application.setStatus(LeaveApplicationStatus.PENDING);
        application.setAppliedOn(LocalDateTime.now());
        application.setReason("Conference");

        when(leaveApplicationRepository.findById(777L)).thenReturn(Optional.of(application));

        EdusyncException ex = assertThrows(
                EdusyncException.class,
                () -> service.approve(777L, 9001L, new LeaveReviewDTO("Not allowed"))
        );

        assertEquals("You cannot approve your own leave application", ex.getMessage());
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
}

