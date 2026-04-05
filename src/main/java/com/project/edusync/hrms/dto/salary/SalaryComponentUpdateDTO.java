package com.project.edusync.hrms.dto.salary;

import com.project.edusync.hrms.model.enums.SalaryCalculationMethod;
import com.project.edusync.hrms.model.enums.SalaryComponentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SalaryComponentUpdateDTO(
        @NotBlank @Size(max = 40) String componentCode,
        @NotBlank @Size(max = 120) String componentName,
        @NotNull SalaryComponentType type,
        @NotNull SalaryCalculationMethod calculationMethod,
        @NotNull @DecimalMin("0.00") BigDecimal defaultValue,
        Boolean isTaxable,
        Boolean isStatutory,
        Integer sortOrder
) {
}

