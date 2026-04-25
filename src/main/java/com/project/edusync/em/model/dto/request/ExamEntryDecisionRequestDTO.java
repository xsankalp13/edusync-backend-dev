package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExamEntryDecisionRequestDTO {

    @NotNull(message = "Exam schedule ID is required")
    private Long examScheduleId;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Allowed flag is required")
    private Boolean allowed;

    @Size(max = 300, message = "Reason must be at most 300 characters")
    private String reason;
}

