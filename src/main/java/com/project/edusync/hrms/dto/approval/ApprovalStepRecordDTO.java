package com.project.edusync.hrms.dto.approval;

import com.project.edusync.hrms.model.enums.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApprovalStepRecordDTO(
        int stepOrder,
        String approverRole,
        UUID approverRef,
        ApprovalStatus status,
        String remarks,
        LocalDateTime actedAt
) {}

