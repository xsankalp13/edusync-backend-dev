package com.project.edusync.hrms.dto.approval;

import com.project.edusync.hrms.model.enums.ApprovalActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApprovalChainConfigCreateDTO(
        @NotBlank String chainName,
        @NotNull ApprovalActionType actionType,
        @Valid @NotNull List<ApprovalChainStepDTO> steps
) {}

