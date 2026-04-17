package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.TemplateQuestionType;
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
public class EvaluationStructureResponseDTO {
    private List<EvaluationSectionDTO> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationSectionDTO {
        private String name;
        private Integer totalQuestions;
        private Integer attemptQuestions;
        private TemplateSectionType sectionType;
        private String helperText;
        private List<EvaluationQuestionDTO> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationQuestionDTO {
        private Integer qNo;
        private Integer maxMarks;
        private TemplateQuestionType type;
        private List<String> options;
    }
}

