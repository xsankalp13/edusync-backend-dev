package com.project.edusync.hrms.dto.leavetemplate;

import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LeaveTemplateUpdateDTO(
        @NotBlank @Size(max = 150) String templateName,
        @Size(max = 500) String description,
        StaffCategory applicableCategory,
        @Valid List<LeaveTemplateItemRequestDTO> items
) {
}
