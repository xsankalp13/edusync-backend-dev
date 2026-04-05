package com.project.edusync.hrms.dto.payroll;

import java.math.BigDecimal;

public record PayslipLineItemDTO(
        String componentCode,
        String componentName,
        String type,
        BigDecimal amount
) {
}

