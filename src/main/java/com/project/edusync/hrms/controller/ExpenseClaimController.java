package com.project.edusync.hrms.controller;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.hrms.dto.document.DocumentUploadInitResponseDTO;
import com.project.edusync.hrms.dto.expense.ExpenseDTOs.*;
import com.project.edusync.hrms.model.enums.ExpenseStatus;
import com.project.edusync.hrms.service.impl.ExpenseServiceImpl;
import com.project.edusync.uis.repository.StaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/expense-claims")
@RequiredArgsConstructor
@Tag(name = "HRMS Expense Claims", description = "Staff expense claim management")
public class ExpenseClaimController {

    private final ExpenseServiceImpl expenseService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create expense claim")
    public ResponseEntity<ClaimResponseDTO> create(@RequestBody ClaimCreateDTO dto) {
        return ResponseEntity.ok(expenseService.createClaim(dto));
    }

    @PostMapping("/self/claims")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create own expense claim")
    public ResponseEntity<ClaimResponseDTO> createSelf(@RequestBody ClaimCreateDTO dto) {
        ClaimCreateDTO selfDto = new ClaimCreateDTO(resolveCurrentStaffRef(), dto.title(), dto.description());
        return ResponseEntity.ok(expenseService.createClaim(selfDto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List expense claims")
    public ResponseEntity<List<ClaimResponseDTO>> list(
            @RequestParam(required = false) String staffRef,
            @RequestParam(required = false) ExpenseStatus status) {
        return ResponseEntity.ok(expenseService.listClaims(staffRef, status));
    }

    @GetMapping("/self/claims")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List own expense claims")
    public ResponseEntity<List<ClaimResponseDTO>> listSelf(@RequestParam(required = false) ExpenseStatus status) {
        return ResponseEntity.ok(expenseService.listClaims(resolveCurrentStaffRef(), status));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get claim detail")
    public ResponseEntity<ClaimResponseDTO> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(expenseService.getClaim(uuid));
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Update claim status")
    public ResponseEntity<ClaimResponseDTO> updateStatus(@PathVariable UUID uuid, @RequestBody ClaimStatusUpdateDTO dto) {
        return ResponseEntity.ok(expenseService.updateStatus(uuid, dto));
    }

    @PostMapping("/{uuid}/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add item to claim")
    public ResponseEntity<ClaimItemResponseDTO> addItem(@PathVariable UUID uuid, @RequestBody ClaimItemCreateDTO dto) {
        return ResponseEntity.ok(expenseService.addItem(uuid, dto));
    }

    @PostMapping("/self/claims/{uuid}/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add item to own claim")
    public ResponseEntity<ClaimItemResponseDTO> addSelfItem(@PathVariable UUID uuid, @RequestBody ClaimItemCreateDTO dto) {
        String staffRef = resolveCurrentStaffRef();
        assertClaimOwnership(uuid, staffRef);
        return ResponseEntity.ok(expenseService.addItem(uuid, dto));
    }

    @PatchMapping("/self/claims/{uuid}/submit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit own draft claim")
    public ResponseEntity<ClaimResponseDTO> submitSelf(@PathVariable UUID uuid) {
        String staffRef = resolveCurrentStaffRef();
        assertClaimOwnership(uuid, staffRef);
        return ResponseEntity.ok(expenseService.updateStatus(uuid, new ClaimStatusUpdateDTO(ExpenseStatus.SUBMITTED, null)));
    }

    @PutMapping("/{uuid}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update claim item")
    public ResponseEntity<ClaimItemResponseDTO> updateItem(@PathVariable UUID uuid, @PathVariable Long itemId,
                                                            @RequestBody ClaimItemUpdateDTO dto) {
        return ResponseEntity.ok(expenseService.updateItem(uuid, itemId, dto));
    }

    @DeleteMapping("/{uuid}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete claim item")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID uuid, @PathVariable Long itemId) {
        expenseService.deleteItem(uuid, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/self/claims/receipt/upload-init")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get pre-signed URL for expense receipt upload")
    public ResponseEntity<DocumentUploadInitResponseDTO> initiateReceiptUpload(
            @Valid @RequestBody ReceiptUploadInitRequestDTO dto) {
        return ResponseEntity.ok(expenseService.initiateReceiptUpload(dto));
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

    private void assertClaimOwnership(UUID claimUuid, String currentStaffRef) {
        ClaimResponseDTO claim = expenseService.getClaim(claimUuid);
        if (claim.staffRef() == null || !claim.staffRef().toString().equals(currentStaffRef)) {
            throw new EdusyncException("You can only modify your own claims", HttpStatus.FORBIDDEN);
        }
    }
}

