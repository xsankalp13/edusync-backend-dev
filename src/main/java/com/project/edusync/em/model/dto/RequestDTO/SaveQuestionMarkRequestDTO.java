package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.AnnotationType;
import jakarta.validation.constraints.DecimalMin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveQuestionMarkRequestDTO {

    @jakarta.validation.constraints.NotBlank(message = "sectionName is required")
    private String sectionName;

    @jakarta.validation.constraints.NotNull(message = "questionNumber is required")
    private Integer questionNumber;

    private String optionLabel;

    @DecimalMin(value = "0.0", inclusive = true, message = "marksObtained cannot be negative")
    private BigDecimal marksObtained;

    private AnnotationType annotationType = AnnotationType.NONE;
}

