package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.TemplateSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSnapshotSectionDTO {
    private String name;
    private Integer sectionOrder;
    private Integer questionCount;
    private Integer marksPerQuestion;
    private Integer totalQuestions;
    private Integer attemptQuestions;
    private TemplateSectionType sectionType;
    private Boolean internalChoiceEnabled;
    private List<TemplateSnapshotQuestionDTO> questions;
}

