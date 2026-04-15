package com.project.edusync.hrms.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PromotionCreateDTO(
        @NotBlank String staffRef,
        @NotBlank String proposedDesignationRef,
        String proposedGradeRef,
        String newSalaryTemplateRef,
        @NotNull LocalDate effectiveDate,
        @Size(max = 100) String orderReference,
        @Size(max = 500) String remarks
) {
}
