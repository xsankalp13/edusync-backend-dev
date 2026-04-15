package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;

public record ComplianceSummaryDTO(
        String financialYear,
        boolean pfConfigured, BigDecimal totalPfContributions,
        boolean esiConfigured, BigDecimal totalEsiContributions,
        boolean ptConfigured, BigDecimal totalPtDeducted,
        int totalPayrollMonths, BigDecimal totalNetPayroll
) {}

