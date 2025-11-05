package com.project.edusync.em.model.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for returning a complete GradeSystem with its scales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSystemResponseDTO {

    private UUID uuid;
    private String systemName;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Set<GradeScaleResponseDTO> gradeScales;
}