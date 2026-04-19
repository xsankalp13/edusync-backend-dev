package com.project.edusync.hrms.dto.promotion;

import com.project.edusync.hrms.model.enums.PromotionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PromotionResponseDTO(
        Long promotionId,
        String uuid,
        Long staffId,
        String staffName,
        String employeeId,
        Long currentDesignationId,
        String currentDesignationCode,
        String currentDesignationName,
        Long proposedDesignationId,
        String proposedDesignationCode,
        String proposedDesignationName,
        Long currentGradeId,
        String currentGradeCode,
        Long proposedGradeId,
        String proposedGradeCode,
        Long newSalaryTemplateId,
        String newSalaryTemplateName,
        LocalDate effectiveDate,
        PromotionStatus status,
        Long initiatedByUserId,
        Long approvedByUserId,
        String approvedByName,
        LocalDateTime approvedOn,
        String orderReference,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
