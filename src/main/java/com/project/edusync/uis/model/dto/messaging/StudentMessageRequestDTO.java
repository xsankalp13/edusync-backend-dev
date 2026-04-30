package com.project.edusync.uis.model.dto.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentMessageRequestDTO {
    @NotNull
    private Long receiverUserId;

    @NotBlank
    private String content;
}

