package com.project.edusync.hrms.dto.salary;

import com.project.edusync.uis.model.enums.StaffCategory;
import java.time.LocalDateTime;
import java.util.List;

public record SalaryTemplateResponseDTO(
        Long templateId,
        String uuid,
        String templateName,
        String description,
        Long gradeId,
        String gradeCode,
        String gradeName,
        String academicYear,
        StaffCategory applicableCategory,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TemplateComponentResponseDTO> components
) {
}

