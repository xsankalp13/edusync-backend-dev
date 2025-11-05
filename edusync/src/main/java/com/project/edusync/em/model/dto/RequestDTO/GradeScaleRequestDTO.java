package com.project.edusync.em.model.dto.RequestDTO;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Nested DTO for a single rule within a GradeSystem.
 * This is not expected to be created on its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScaleRequestDTO {

    @NotBlank(message = "Grade name is required")
    private String gradeName;

    @NotNull(message = "Min percentage is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal minPercentage;

    @NotNull(message = "Max percentage is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal maxPercentage;

    // Optional grade point
    @DecimalMin(value = "0.0")
    private BigDecimal gradePoints;
}