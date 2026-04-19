package com.project.edusync.hrms.dto.leavetemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StaffLeaveTemplateMappingResponseDTO(
        Long mappingId,
        String uuid,
        Long staffId,
        String employeeId,
        String staffName,
        Long templateId,
        String templateUuid,
        String templateName,
        String academicYear,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        LocalDateTime createdAt
) {
}
