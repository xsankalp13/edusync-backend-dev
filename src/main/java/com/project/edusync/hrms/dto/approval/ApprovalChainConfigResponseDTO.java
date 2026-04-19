package com.project.edusync.hrms.dto.approval;

import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApprovalChainConfigResponseDTO(
        UUID uuid,
        ApprovalActionType actionType,
        String chainName,
        boolean active,
        List<ApprovalChainStepDTO> steps,
        LocalDateTime createdAt
) {}

