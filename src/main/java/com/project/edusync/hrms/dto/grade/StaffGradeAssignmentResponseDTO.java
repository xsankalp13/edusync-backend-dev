package com.project.edusync.hrms.dto.grade;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StaffGradeAssignmentResponseDTO(
        Long assignmentId,
        String uuid,
        Long staffId,
        String staffName,
        Long gradeId,
        String gradeCode,
        String gradeName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String promotionOrderRef,
        Long promotedByStaffId,
        String remarks,
        LocalDateTime createdAt
) {
}

