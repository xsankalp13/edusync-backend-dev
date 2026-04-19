package com.project.edusync.hrms.controller;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.hrms.dto.loan.LoanDTOs.*;
import com.project.edusync.hrms.model.enums.LoanStatus;
import com.project.edusync.hrms.service.impl.LoanServiceImpl;
import com.project.edusync.uis.repository.StaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/loans")
@RequiredArgsConstructor
@Tag(name = "HRMS Loans", description = "Staff loan & advance management")
public class LoanController {

    private final LoanServiceImpl loanService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Apply for loan")
    public ResponseEntity<LoanResponseDTO> apply(@RequestBody LoanApplicationDTO dto) {
        return ResponseEntity.ok(loanService.applyLoan(dto));
    }

    @PostMapping("/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply for own loan")
    public ResponseEntity<LoanResponseDTO> applySelf(@RequestBody LoanApplicationDTO dto) {
        LoanApplicationDTO selfDto = new LoanApplicationDTO(
                resolveCurrentStaffRef(),
                dto.loanType(),
                dto.principalAmount(),
                dto.emiCount(),
                dto.interestRate(),
                dto.reason()
        );
        return ResponseEntity.ok(loanService.applyLoan(selfDto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List loans")
    public ResponseEntity<List<LoanResponseDTO>> list(
            @RequestParam(required = false) String staffRef,
            @RequestParam(required = false) LoanStatus status) {
        return ResponseEntity.ok(loanService.listLoans(staffRef, status));
    }

    @GetMapping("/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List own loans")
    public ResponseEntity<List<LoanResponseDTO>> listSelf(
            @RequestParam(required = false) LoanStatus status) {
        return ResponseEntity.ok(loanService.listLoans(resolveCurrentStaffRef(), status));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loan detail")
    public ResponseEntity<LoanResponseDTO> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(loanService.getLoan(uuid));
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Update loan status (approve/disburse)")
    public ResponseEntity<LoanResponseDTO> updateStatus(@PathVariable UUID uuid, @RequestBody LoanStatusUpdateDTO dto) {
        return ResponseEntity.ok(loanService.updateLoanStatus(uuid, dto));
    }

    @GetMapping("/{uuid}/repayments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List repayments for a loan")
    public ResponseEntity<List<RepaymentDTO>> repayments(@PathVariable UUID uuid) {
        return ResponseEntity.ok(loanService.listRepayments(uuid));
    }

    @GetMapping("/{uuid}/document")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download loan sanction letter PDF")
    public ResponseEntity<byte[]> loanDocument(@PathVariable UUID uuid) {
        byte[] pdf = loanService.getLoanDocumentPdf(uuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan-sanction-letter-" + uuid + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{uuid}/repayments/manual")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Add manual repayment")
    public ResponseEntity<RepaymentDTO> manualRepayment(@PathVariable UUID uuid, @RequestBody ManualRepaymentDTO dto) {
        return ResponseEntity.ok(loanService.manualRepayment(uuid, dto));
    }

    @GetMapping("/summary/{staffRef}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loan summary for staff")
    public ResponseEntity<LoanSummaryDTO> summary(@PathVariable String staffRef) {
        return ResponseEntity.ok(loanService.getLoanSummary(staffRef));
    }

    @GetMapping("/summary/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get own loan summary")
    public ResponseEntity<LoanSummaryDTO> selfSummary() {
        return ResponseEntity.ok(loanService.getLoanSummary(resolveCurrentStaffRef()));
    }

    private String resolveCurrentStaffRef() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
                .map(staff -> staff.getUuid().toString())
                .orElseThrow(() -> new EdusyncException(
                        "Authenticated user is not linked to a staff profile",
                        HttpStatus.FORBIDDEN
                ));
    }
}

