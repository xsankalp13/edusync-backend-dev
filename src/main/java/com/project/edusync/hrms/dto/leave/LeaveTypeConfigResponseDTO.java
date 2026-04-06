package com.project.edusync.hrms.dto.leave;

import com.project.edusync.uis.model.enums.StaffCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

public record LeaveTypeConfigResponseDTO(
        @Schema(example = "1")
        Long leaveTypeId,
        @Schema(example = "0c0a68ef-3ce7-4af2-8a27-8d00e18ca2a3")
        String uuid,
        @Schema(example = "CL")
        String leaveCode,
        @Schema(example = "Casual Leave")
        String displayName,
        String description,
        @Schema(example = "12")
        Integer annualQuota,
        boolean carryForwardAllowed,
        Integer maxCarryForward,
        boolean encashmentAllowed,
        Integer minDaysBeforeApply,
        Integer maxConsecutiveDays,
        boolean requiresDocument,
        Integer documentRequiredAfterDays,
        boolean isPaid,
        Set<StaffCategory> applicableCategories,
        Set<String> applicableGrades,
        @Schema(example = "true")
        boolean active,
        @Schema(example = "10")
        Integer sortOrder,
        @Schema(type = "string", format = "date-time", example = "2026-04-04T10:15:30")
        LocalDateTime createdAt,
        @Schema(type = "string", format = "date-time", example = "2026-04-04T10:15:30")
        LocalDateTime updatedAt
) {
}

