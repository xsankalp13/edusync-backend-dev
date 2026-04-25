package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamControllerAssignmentRequestDTO {

    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotNull(message = "Staff ID is required")
    private Long staffId;
}

