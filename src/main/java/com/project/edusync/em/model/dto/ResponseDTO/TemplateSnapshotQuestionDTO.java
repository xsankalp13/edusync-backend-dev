package com.project.edusync.em.model.dto.ResponseDTO;

import com.project.edusync.em.model.enums.TemplateQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSnapshotQuestionDTO {
    private Integer questionNo;
    private Integer marks;
    private TemplateQuestionType type;
    private List<String> options;
}

