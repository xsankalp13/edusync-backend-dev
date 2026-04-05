package com.project.edusync.hrms.dto.salary;

import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalaryComponentResponseDTO(
        Long componentId,
        String uuid,
        String componentCode,
        String componentName,
        SalaryComponentType type,
        SalaryCalculationMethod calculationMethod,
        BigDecimal defaultValue,
        boolean isTaxable,
        boolean isStatutory,
        Integer sortOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

