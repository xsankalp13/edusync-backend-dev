package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ExamControllerAssignmentResponseDTO {
    Long assignmentId;
    Long examId;
    Long staffId;
    String staffName;
    Long assignedByUserId;
    LocalDateTime assignedAt;
    Integer remainingAttempts;
}

