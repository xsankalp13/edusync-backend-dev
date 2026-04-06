package com.project.edusync.hrms.dto.dashboard;

import java.math.BigDecimal;

public record MonthlyPayrollTrendItem(
        String month,
        BigDecimal amount
) {
}

