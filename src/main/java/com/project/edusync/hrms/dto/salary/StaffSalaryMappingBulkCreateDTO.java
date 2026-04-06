package com.project.edusync.hrms.dto.salary;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record StaffSalaryMappingBulkCreateDTO(
        @NotBlank String templateRef,
        @NotEmpty List<@NotBlank String> staffRefs,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Size(max = 500) String remarks
) {
}

