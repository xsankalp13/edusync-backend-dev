package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.TemplateSectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSectionRequestDTO {

    @NotBlank(message = "Section name is required")
    private String sectionName;

    private Integer sectionOrder;

    @Min(value = 1, message = "questionCount must be at least 1")
    private Integer questionCount;

    @Min(value = 1, message = "totalQuestions must be at least 1")
    private Integer totalQuestions;

    @Min(value = 1, message = "attemptQuestions must be at least 1")
    private Integer attemptQuestions;

    private TemplateSectionType sectionType;

    private Boolean internalChoiceEnabled;

    @Min(value = 1, message = "marksPerQuestion must be at least 1")
    private Integer marksPerQuestion;

    @Valid
    private List<TemplateQuestionRequestDTO> questions;

    private Boolean isObjective;
    private Boolean isSubjective;
}

