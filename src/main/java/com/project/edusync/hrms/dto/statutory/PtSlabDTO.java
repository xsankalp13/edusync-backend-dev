package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;

public record PtSlabDTO(
        BigDecimal minSalary,
        BigDecimal maxSalary,
        BigDecimal monthlyTax
) {}

