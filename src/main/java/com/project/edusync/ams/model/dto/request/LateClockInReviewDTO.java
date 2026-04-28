package com.project.edusync.ams.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LateClockInReviewDTO {
    @NotBlank
    private String action;   // "APPROVE" | "REJECT"
    private String remarks;
}
