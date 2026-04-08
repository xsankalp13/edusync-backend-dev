package com.project.edusync.teacher.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureLogResponseDto {
    private UUID uuid;
    private UUID scheduleUuid;
    private String title;
    private String description;
    private List<String> documentUrls;
    private boolean hasTakenTest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
