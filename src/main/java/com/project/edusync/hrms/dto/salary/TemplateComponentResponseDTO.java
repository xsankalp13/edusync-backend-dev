package com.project.edusync.hrms.dto.salary;

import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;

import java.math.BigDecimal;

public record TemplateComponentResponseDTO(
        Long componentId,
        String componentCode,
        String componentName,
        SalaryComponentType type,
        SalaryCalculationMethod calculationMethod,
        BigDecimal value
) {
}

