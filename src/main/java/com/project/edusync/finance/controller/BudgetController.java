package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.budget.*;
import com.project.edusync.finance.model.enums.BudgetStatus;
import com.project.edusync.finance.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Departmental Budget management.
 * Base path: /auth/finance/budgets
 */
@RestController
@RequestMapping("${api.url}/auth/finance/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // TODO: Replace with JWT-derived school ID
    private static final Long DEFAULT_SCHOOL_ID = 1L;

    // ── List & Filter ─────────────────────────────────────────────────────────

    /**
     * GET /auth/finance/budgets
     * List all budgets (summary). Optionally filter by year or status.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:budget:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<List<BudgetSummaryDTO>> getAll(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) BudgetStatus status
    ) {
        if (academicYear != null) {
            return ResponseEntity.ok(budgetService.getBudgetsByYear(academicYear, DEFAULT_SCHOOL_ID));
        }
        if (status != null) {
            return ResponseEntity.ok(budgetService.getBudgetsByStatus(status, DEFAULT_SCHOOL_ID));
        }
        return ResponseEntity.ok(budgetService.getAllBudgets(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/budgets/{id}
     * Full budget detail with all line items and variance data.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:budget:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<BudgetResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id, DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/budgets/meta/years
     * Distinct academic years in use — for the year filter dropdown.
     */
    @GetMapping("/meta/years")
    @PreAuthorize("hasAnyAuthority('finance:budget:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<String>> getAcademicYears() {
        return ResponseEntity.ok(budgetService.getAcademicYears(DEFAULT_SCHOOL_ID));
    }

    /**
     * GET /auth/finance/budgets/meta/departments
     * Distinct departments in use — for the department filter dropdown.
     */
    @GetMapping("/meta/departments")
    @PreAuthorize("hasAnyAuthority('finance:budget:read', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<List<String>> getDepartments() {
        return ResponseEntity.ok(budgetService.getDepartments(DEFAULT_SCHOOL_ID));
    }

    // ── Create & Update ───────────────────────────────────────────────────────

    /**
     * POST /auth/finance/budgets
     * Create a new DRAFT budget.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('finance:budget:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> create(@Valid @RequestBody BudgetCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * PUT /auth/finance/budgets/{id}
     * Update a DRAFT or REVISION_REQUESTED budget.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:budget:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody BudgetCreateDTO dto) {
        return ResponseEntity.ok(budgetService.updateBudget(id, dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * DELETE /auth/finance/budgets/{id}
     * Delete a DRAFT budget.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:budget:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.deleteBudget(id, DEFAULT_SCHOOL_ID);
        return ResponseEntity.noContent().build();
    }

    // ── Lifecycle Actions ─────────────────────────────────────────────────────

    /**
     * POST /auth/finance/budgets/{id}/submit
     * Submit a DRAFT budget for Finance Admin review.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('finance:budget:write', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> submit(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.submitBudget(id, DEFAULT_SCHOOL_ID));
    }

    /**
     * POST /auth/finance/budgets/{id}/review
     * Finance Admin approves or rejects a SUBMITTED budget.
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('finance:budget:approve', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> review(
            @PathVariable Long id,
            @Valid @RequestBody BudgetApprovalDTO dto) {
        return ResponseEntity.ok(budgetService.reviewBudget(id, dto, DEFAULT_SCHOOL_ID));
    }

    /**
     * POST /auth/finance/budgets/{id}/request-revision
     * Send a SUBMITTED budget back for revision.
     */
    @PostMapping("/{id}/request-revision")
    @PreAuthorize("hasAnyAuthority('finance:budget:approve', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> requestRevision(
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(budgetService.requestRevision(id, notes, DEFAULT_SCHOOL_ID));
    }

    /**
     * POST /auth/finance/budgets/{id}/close
     * Close an APPROVED budget at year end.
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('finance:budget:approve', 'ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN', 'ROLE_FINANCE_ADMIN')")
    public ResponseEntity<BudgetResponseDTO> close(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.closeBudget(id, DEFAULT_SCHOOL_ID));
    }
}
