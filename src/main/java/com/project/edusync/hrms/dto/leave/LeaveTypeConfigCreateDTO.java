package com.project.edusync.hrms.dto.leave;

import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record LeaveTypeConfigCreateDTO(
        @NotBlank @Size(max = 20) String leaveCode,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 500) String description,
        @NotNull @Min(0) Integer annualQuota,
        Boolean carryForwardAllowed,
        @Min(0) Integer maxCarryForward,
        Boolean encashmentAllowed,
        @Min(0) Integer minDaysBeforeApply,
        @Min(1) Integer maxConsecutiveDays,
        Boolean requiresDocument,
        @Min(1) Integer documentRequiredAfterDays,
        Boolean isPaid,
        Set<StaffCategory> applicableCategories,
        Set<String> applicableGrades,
        Integer sortOrder
) {
}

