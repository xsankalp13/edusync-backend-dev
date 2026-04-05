package com.project.edusync.hrms.dto.grade;

import com.project.edusync.hrms.model.enums.TeachingWing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StaffGradeCreateDTO(
        @NotBlank @Size(max = 30) String gradeCode,
        @NotBlank @Size(max = 120) String gradeName,
        @NotNull TeachingWing teachingWing,
        @NotNull @DecimalMin("0.00") BigDecimal payBandMin,
        @NotNull @DecimalMin("0.00") BigDecimal payBandMax,
        @NotNull @Min(1) Integer sortOrder,
        @Min(0) Integer minYearsForPromotion,
        @Size(max = 500) String description
) {
}

