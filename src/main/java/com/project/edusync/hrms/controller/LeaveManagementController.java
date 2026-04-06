package com.project.edusync.hrms.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveApplicationResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceInitRequestDTO;
import com.project.edusync.hrms.dto.leave.LeaveBalanceResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveReviewDTO;
import com.project.edusync.hrms.exception.LeaveAccessDeniedException;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.hrms.service.LeaveManagementService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.repository.StaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/leaves")
@RequiredArgsConstructor
@Tag(name = "HRMS Leave Management", description = "Leave application and leave balance APIs")
public class LeaveManagementController {

    private final LeaveManagementService leaveManagementService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;

    @GetMapping("/applications")
    @Operation(summary = "List leave applications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LeaveApplicationResponseDTO>> listApplications(
            @RequestParam(required = false) String staffIdentifier,
            @RequestParam(required = false) LeaveApplicationStatus status,
            @RequestParam(required = false) String leaveTypeCode,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            Authentication authentication,
            Pageable pageable
    ) {
        Long currentUserId = authUtil.getCurrentUserId();
        boolean canViewAll = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> authority.equals("ROLE_SUPER_ADMIN")
                        || authority.equals("ROLE_SCHOOL_ADMIN")
                        || authority.equals("ROLE_ADMIN"));

        Long staffId = null;
        if (staffIdentifier != null && !staffIdentifier.isBlank()) {
            Staff staff = PublicIdentifierResolver.resolve(
                    staffIdentifier,
                    staffRepository::findByUuid,
                    staffRepository::findById,
                    "Staff"
            );
            staffId = staff.getId();
        }

        return ResponseEntity.ok(leaveManagementService.listApplications(
                currentUserId,
                canViewAll,
                staffId,
                status,
                leaveTypeCode,
                fromDate,
                toDate,
                pageable
        ));
    }

    @GetMapping("/applications/{identifier}")
    @Operation(summary = "Get leave application by identifier")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN','ROLE_PRINCIPAL','ROLE_TEACHER','ROLE_LIBRARIAN')")
    public ResponseEntity<LeaveApplicationResponseDTO> getApplicationByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(leaveManagementService.getApplicationByIdentifier(identifier));
    }

    @PostMapping("/applications")
    @Operation(summary = "Apply for leave as current staff")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<LeaveApplicationResponseDTO> applyForLeave(@Valid @RequestBody LeaveApplicationCreateDTO dto) {
        return new ResponseEntity<>(leaveManagementService.applyForCurrentStaff(dto), HttpStatus.CREATED);
    }

    @PostMapping("/applications/{identifier}/approve")
    @Operation(summary = "Approve leave application")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN','ROLE_PRINCIPAL')")
    public ResponseEntity<LeaveApplicationResponseDTO> approve(
            @PathVariable String identifier,
            @RequestBody(required = false) LeaveReviewDTO dto
    ) {
        return ResponseEntity.ok(leaveManagementService.approveByIdentifier(identifier, authUtil.getCurrentUserId(), dto));
    }

    @PostMapping("/applications/{identifier}/reject")
    @Operation(summary = "Reject leave application")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN','ROLE_PRINCIPAL')")
    public ResponseEntity<LeaveApplicationResponseDTO> reject(
            @PathVariable String identifier,
            @RequestBody(required = false) LeaveReviewDTO dto
    ) {
        return ResponseEntity.ok(leaveManagementService.rejectByIdentifier(identifier, authUtil.getCurrentUserId(), dto));
    }

    @PostMapping("/applications/{identifier}/cancel")
    @Operation(summary = "Cancel own leave application")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<LeaveApplicationResponseDTO> cancel(@PathVariable String identifier) {
        return ResponseEntity.ok(leaveManagementService.cancelByCurrentStaffIdentifier(identifier));
    }

    @GetMapping("/balance/me")
    @Operation(summary = "Get leave balance for current staff")
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_PRINCIPAL','ROLE_LIBRARIAN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<LeaveBalanceResponseDTO>> getMyBalance(@RequestParam(required = false) String academicYear) {
        Long currentUserId = authUtil.getCurrentUserId();
        Long staffId = staffRepository.findByUserProfile_User_Id(currentUserId)
                .orElseThrow(() -> new LeaveAccessDeniedException("Authenticated user is not linked to a staff profile"))
                .getId();
        return ResponseEntity.ok(leaveManagementService.getBalanceForStaff(staffId, academicYear));
    }

    @GetMapping("/balance/{staffIdentifier}")
    @Operation(summary = "Get leave balance for a staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN','ROLE_PRINCIPAL')")
    public ResponseEntity<List<LeaveBalanceResponseDTO>> getBalanceForStaff(
            @PathVariable String staffIdentifier,
            @RequestParam(required = false) String academicYear
    ) {
        return ResponseEntity.ok(leaveManagementService.getBalanceForStaffIdentifier(staffIdentifier, academicYear));
    }

    @GetMapping("/balance/all")
    @Operation(summary = "Get leave balance for all staff")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN','ROLE_PRINCIPAL')")
    public ResponseEntity<Page<LeaveBalanceResponseDTO>> getAllBalances(
            @RequestParam(required = false) String academicYear,
            Pageable pageable
    ) {
        return ResponseEntity.ok(leaveManagementService.getAllBalances(academicYear, pageable));
    }

    @PostMapping("/balance/initialize")
    @Operation(summary = "Initialize leave balance for an academic year")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<BulkOperationResultDTO> initializeBalances(@Valid @RequestBody LeaveBalanceInitRequestDTO request) {
        return new ResponseEntity<>(leaveManagementService.initializeBalances(request), HttpStatus.CREATED);
    }
}

