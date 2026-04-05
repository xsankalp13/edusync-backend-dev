package com.project.edusync.hrms.dto.payroll;

import com.project.edusync.hrms.model.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayslipSummaryDTO(
        Long payslipId,
        String uuid,
        Long payrollRunId,
        Long staffId,
        String staffName,
        String employeeId,
        Integer payMonth,
        Integer payYear,
        BigDecimal grossPay,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        PayrollRunStatus status,
        LocalDateTime generatedAt
) {
}

