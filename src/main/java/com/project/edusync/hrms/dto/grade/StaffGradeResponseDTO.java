package com.project.edusync.hrms.dto.grade;

import com.project.edusync.hrms.model.enums.TeachingWing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StaffGradeResponseDTO(
        Long gradeId,
        String uuid,
        String gradeCode,
        String gradeName,
        TeachingWing teachingWing,
        BigDecimal payBandMin,
        BigDecimal payBandMax,
        Integer sortOrder,
        Integer minYearsForPromotion,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

