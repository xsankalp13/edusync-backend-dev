package com.project.edusync.hrms.dto.salary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ComponentOverrideDTO(
        @NotBlank String componentRef,
        @NotNull @DecimalMin("0.00") BigDecimal overrideValue,
        @Size(max = 500) String reason
) {
}

