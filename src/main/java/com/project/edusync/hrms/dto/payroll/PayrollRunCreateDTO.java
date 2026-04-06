package com.project.edusync.hrms.dto.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PayrollRunCreateDTO(
        @NotNull @Min(2000) Integer payYear,
        @NotNull @Min(1) @Max(12) Integer payMonth,
        @Size(max = 500) String remarks
) {
}

