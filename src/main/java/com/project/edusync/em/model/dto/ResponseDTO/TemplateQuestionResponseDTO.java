package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.TemplateQuestionType;
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
public class TemplateQuestionResponseDTO {
    private UUID id;
    private Integer questionNo;
    private Integer marks;
    private TemplateQuestionType type;
    private List<String> options;
}

