package com.project.edusync.discipline.model.dto;

import com.project.edusync.discipline.model.enums.RemarkTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateRemarkRequest {

    @NotNull(message = "Student UUID is required")
    private UUID studentUuid;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Tag is required")
    private RemarkTag tag;

    /**
     * Optional — defaults to today if not provided.
     */
    private LocalDate remarkDate;
}
