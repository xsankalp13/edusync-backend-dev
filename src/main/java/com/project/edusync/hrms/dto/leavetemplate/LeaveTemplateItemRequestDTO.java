package com.project.edusync.hrms.dto.leavetemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record LeaveTemplateItemRequestDTO(
        @NotBlank String leaveTypeRef,
        @PositiveOrZero Integer customQuota
) {
}
