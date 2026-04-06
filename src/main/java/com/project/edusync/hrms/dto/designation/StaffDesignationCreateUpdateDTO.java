package com.project.edusync.hrms.dto.designation;

import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffDesignationCreateUpdateDTO(
        @NotBlank @Size(max = 20) String designationCode,
        @NotBlank @Size(max = 100) String designationName,
        @NotNull StaffCategory category,
        @Size(max = 500) String description,
        Integer sortOrder
) {
}

