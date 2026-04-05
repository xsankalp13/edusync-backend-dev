package com.project.edusync.hrms.dto.grade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StaffGradeAssignmentCreateDTO(
        @NotBlank String staffRef,
        @NotBlank String gradeRef,
        @NotNull LocalDate effectiveFrom,
        @Size(max = 120) String promotionOrderRef,
        @Size(max = 500) String remarks
) {
}

