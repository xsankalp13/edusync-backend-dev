package com.project.edusync.hrms.dto.promotion;

import jakarta.validation.constraints.Size;

public record PromotionReviewDTO(
        @Size(max = 500) String remarks
) {
}
