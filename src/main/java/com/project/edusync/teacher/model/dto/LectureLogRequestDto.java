package com.project.edusync.teacher.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureLogRequestDto {
    @NotNull(message = "Schedule UUID is required")
    private UUID scheduleUuid;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    
    private List<String> documentUrls;
    
    private boolean hasTakenTest;
}
