package com.project.edusync.em.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ExamEntryDecisionResponseDTO {
    Long examScheduleId;
    Long studentId;
    boolean allowed;
    String reason;
    Long decidedByStaffId;
    LocalDateTime decidedAt;
}

