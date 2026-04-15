package com.project.edusync.hrms.dto.payroll;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a single staff member's row in the Bank Salary Advice document.
 * Contains both salary totals and bank account details needed for NEFT/RTGS transfer.
 */
public record BankAdviceStaffEntryDTO(
        int serialNo,
        String employeeId,
        String staffName,
        String designation,
        String department,
        String bankName,
        String bankAccountNumber,
        String bankIfscCode,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        List<PayslipLineItemDTO> earningLines,
        List<PayslipLineItemDTO> deductionLines
) {
    /** Pre-computed map of componentCode → amount for use in Thymeleaf templates (avoids SpEL lambda limitation). */
    public Map<String, BigDecimal> deductionMap() {
        if (deductionLines == null || deductionLines.isEmpty()) return Map.of();
        return deductionLines.stream()
                .collect(Collectors.toMap(
                        PayslipLineItemDTO::componentCode,
                        PayslipLineItemDTO::amount,
                        BigDecimal::add
                ));
    }
}
