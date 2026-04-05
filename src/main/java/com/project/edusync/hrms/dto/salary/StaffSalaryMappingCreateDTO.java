package com.project.edusync.hrms.dto.salary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record StaffSalaryMappingCreateDTO(
        @NotBlank String staffRef,
        @NotBlank String templateRef,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Size(max = 500) String remarks,
        List<@Valid ComponentOverrideDTO> overrides
) {
}

