package com.project.edusync.em.model.entity.snapshot;

import com.project.edusync.em.model.enums.TemplateQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSnapshotQuestion {
    private Integer questionNo;
    private Integer marks;
    private TemplateQuestionType type;

    @Builder.Default
    private List<String> options = new ArrayList<>();
}

