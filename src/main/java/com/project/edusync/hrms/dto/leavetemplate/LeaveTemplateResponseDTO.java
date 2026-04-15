package com.project.edusync.hrms.dto.leavetemplate;

import com.project.edusync.uis.model.enums.StaffCategory;

import java.time.LocalDateTime;
import java.util.List;

public record LeaveTemplateResponseDTO(
        Long templateId,
        String uuid,
        String templateName,
        String description,
        String academicYear,
        StaffCategory applicableCategory,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<LeaveTemplateItemDTO> items
) {
}
