package com.project.edusync.hrms.dto.approval;

import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApprovalRequestDetailDTO(
        UUID uuid,
        ApprovalActionType actionType,
        String entityType,
        UUID entityRef,
        UUID requestedByRef,
        LocalDateTime requestedAt,
        int currentStepOrder,
        ApprovalStatus finalStatus,
        LocalDateTime completedAt,
        List<ApprovalStepRecordDTO> steps
) {}

