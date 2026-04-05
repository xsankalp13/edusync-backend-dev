package com.project.edusync.hrms.dto.salary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TemplateComponentInputDTO(
        @NotBlank String componentRef,
        @NotNull @DecimalMin("0.00") BigDecimal value
) {
}

