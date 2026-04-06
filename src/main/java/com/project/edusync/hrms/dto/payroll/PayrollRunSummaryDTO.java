package com.project.edusync.hrms.dto.payroll;

import com.project.edusync.hrms.model.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollRunSummaryDTO(
        Long runId,
        String runUuid,
        Integer payYear,
        Integer payMonth,
        PayrollRunStatus status,
        Integer totalStaff,
        BigDecimal totalGross,
        BigDecimal totalDeductions,
        BigDecimal totalNet,
        LocalDateTime processedOn
) {
}

