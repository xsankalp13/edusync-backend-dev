package com.project.edusync.hrms.dto.payroll;

import java.math.BigDecimal;

public record PayrollRunEntryResponseDTO(
        Long entryId,
        Long staffId,
        String staffName,
        String employeeId,
        Long mappingId,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        String remarks
) {
}

