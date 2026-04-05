package com.project.edusync.hrms.dto.salary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StaffSalaryMappingResponseDTO(
        Long mappingId,
        String uuid,
        Long staffId,
        String staffName,
        String employeeId,
        Long templateId,
        String templateName,
        Long gradeId,
        String gradeCode,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String remarks,
        boolean active,
        LocalDateTime createdAt,
        List<ComponentOverrideDTO> overrides
) {
}

