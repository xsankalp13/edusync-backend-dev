package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceInitRequestDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveReviewDTO;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LeaveManagementService {

    Page<LeaveApplicationResponseDTO> listApplications(
            Long currentUserId,
            boolean canViewAll,
            Long staffId,
            LeaveApplicationStatus status,
            String leaveTypeCode,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    LeaveApplicationResponseDTO getApplicationById(Long applicationId);

    LeaveApplicationResponseDTO getApplicationByIdentifier(String identifier);

    LeaveApplicationResponseDTO applyForCurrentStaff(LeaveApplicationCreateDTO dto);

    LeaveApplicationResponseDTO approve(Long applicationId, Long reviewerUserId, LeaveReviewDTO dto);

    LeaveApplicationResponseDTO approveByIdentifier(String identifier, Long reviewerUserId, LeaveReviewDTO dto);

    LeaveApplicationResponseDTO reject(Long applicationId, Long reviewerUserId, LeaveReviewDTO dto);

    LeaveApplicationResponseDTO rejectByIdentifier(String identifier, Long reviewerUserId, LeaveReviewDTO dto);

    LeaveApplicationResponseDTO cancelByCurrentStaff(Long applicationId);

    LeaveApplicationResponseDTO cancelByCurrentStaffIdentifier(String identifier);

    List<LeaveBalanceResponseDTO> getBalanceForStaff(Long staffId, String academicYear);

    List<LeaveBalanceResponseDTO> getBalanceForStaffIdentifier(String staffIdentifier, String academicYear);

    Page<LeaveBalanceResponseDTO> getAllBalances(String academicYear, Pageable pageable);

    BulkOperationResultDTO initializeBalances(LeaveBalanceInitRequestDTO request);
}

