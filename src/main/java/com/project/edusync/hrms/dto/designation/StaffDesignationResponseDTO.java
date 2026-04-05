package com.project.edusync.hrms.dto.designation;

import com.project.edusync.uis.model.enums.StaffCategory;

import java.time.LocalDateTime;

public record StaffDesignationResponseDTO(
        Long designationId,
        String uuid,
        String designationCode,
        String designationName,
        StaffCategory category,
        String description,
        Integer sortOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

