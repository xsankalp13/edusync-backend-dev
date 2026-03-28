package com.project.edusync.iam.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkTeacherSubjectAssignmentRequestDTO {

    @NotNull(message = "subjectId is required")
    private UUID subjectId;

    @NotEmpty(message = "teacherIds must contain at least one teacher id")
    private List<Long> teacherIds;
}

