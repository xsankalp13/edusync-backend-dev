package com.project.edusync.hrms.dto.payroll;

import com.project.edusync.hrms.model.enums.PayrollRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollRunResponseDTO(
        Long runId,
        String runUuid,
        Integer payYear,
        Integer payMonth,
        PayrollRunStatus status,
        String remarks,
        Integer totalStaff,
        BigDecimal totalGross,
        BigDecimal totalDeductions,
        BigDecimal totalNet,
        LocalDateTime processedOn,
        List<PayrollRunEntryResponseDTO> entries
) {
}

