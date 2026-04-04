package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * DTO for responding with available Subject (UUID and Name).
 */
@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AvailableSubjectDto {
    private UUID uuid;
    private String name;
    private String subjectCode;
    private String color;
}