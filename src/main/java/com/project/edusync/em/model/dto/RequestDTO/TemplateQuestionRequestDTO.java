package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.TemplateQuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionRequestDTO {

    @NotNull(message = "questionNo is required")
    @Min(value = 1, message = "questionNo must be at least 1")
    private Integer questionNo;

    @NotNull(message = "marks is required")
    @Min(value = 1, message = "marks must be at least 1")
    private Integer marks;

    private TemplateQuestionType type;

    private List<String> options;
}

