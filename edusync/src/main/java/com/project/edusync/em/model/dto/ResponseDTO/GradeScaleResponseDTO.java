package com.project.edusync.em.model.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for returning GradeScale data.
 * This entity is not auditable, so it returns its Long PK.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScaleResponseDTO {

    private Long gradeScaleId;
    private String gradeName;
    private BigDecimal minPercentage;
    private BigDecimal maxPercentage;
    private BigDecimal gradePoints;
}