package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.TemplateSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSectionResponseDTO {
    private UUID id;
    private String sectionName;
    private Integer sectionOrder;
    private Integer questionCount;
    private Integer totalQuestions;
    private Integer attemptQuestions;
    private TemplateSectionType sectionType;
    private Boolean internalChoiceEnabled;
    private Integer marksPerQuestion;
    private Boolean isObjective;
    private Boolean isSubjective;
    private List<TemplateQuestionResponseDTO> questions;
}

