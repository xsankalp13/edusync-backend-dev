package com.project.edusync.hrms.dto.payroll;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level data transfer object for the Bank Salary Advice PDF document.
 * Submitted to the institution's bank to authorise bulk NEFT/RTGS salary transfers.
 */
public record BankSalaryAdviceDTO(
        // Run metadata
        String runIdentifier,
        Integer payMonth,
        Integer payYear,
        String payPeriodLabel,          // e.g. "April 2026"
        LocalDateTime generatedAt,

        // Institution info (sourced from app properties)
        String institutionName,

        // Aggregated totals
        Integer totalStaff,
        BigDecimal totalGross,
        BigDecimal totalDeductions,
        BigDecimal totalNetDisbursable,

        // Per-staff entries
        List<BankAdviceStaffEntryDTO> staffEntries,

        // Deduction-component aggregates (for annexure)
        List<DeductionAggregateLine> deductionAggregates
) {

    /** Aggregated total for a single deduction component across all staff. */
    public record DeductionAggregateLine(
            String componentCode,
            String componentName,
            BigDecimal total
    ) {}
}
