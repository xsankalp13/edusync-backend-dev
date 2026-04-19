package com.project.edusync.hrms.dto.approval;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ApprovalChainStepDTO(
        @Min(1) int stepOrder,
        @NotBlank String approverRole,
        String stepLabel
) {}

