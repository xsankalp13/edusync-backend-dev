package com.project.edusync.hrms.dto.leave;

import jakarta.validation.constraints.NotBlank;

public record LeaveBalanceInitRequestDTO(
        @NotBlank String academicYear,
        String carryForwardFromYear
) {
}

