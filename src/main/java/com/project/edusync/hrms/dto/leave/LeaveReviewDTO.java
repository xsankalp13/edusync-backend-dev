package com.project.edusync.hrms.dto.leave;

import jakarta.validation.constraints.Size;

public record LeaveReviewDTO(
        @Size(max = 500) String remarks
) {
}

