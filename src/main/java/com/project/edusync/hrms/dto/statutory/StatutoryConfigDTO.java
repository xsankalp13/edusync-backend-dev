package com.project.edusync.hrms.dto.statutory;

import java.math.BigDecimal;
import java.util.List;

public record StatutoryConfigDTO(
        String financialYear,
        boolean pfApplicable,
        BigDecimal pfEmployeeRate,
        BigDecimal pfEmployerRate,
        BigDecimal pfCeilingAmount,
        boolean esiApplicable,
        BigDecimal esiEmployeeRate,
        BigDecimal esiEmployerRate,
        BigDecimal esiWageLimit,
        boolean ptApplicable,
        String ptState,
        List<PtSlabDTO> ptSlabs
) {}

