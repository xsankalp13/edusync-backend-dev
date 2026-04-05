package com.project.edusync.hrms.dto.salary;

import java.math.BigDecimal;

public record ComputedComponentDTO(
        String componentCode,
        String componentName,
        String calculationMethod,
        BigDecimal configuredValue,
        BigDecimal computedAmount,
        boolean isOverridden
) {
}

