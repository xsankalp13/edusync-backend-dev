package com.project.edusync.em.model.dto.ResponseDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for returning a complete generated QuestionPaper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPaperResponseDTO {

    private UUID uuid;
    private Long scheduleId;
    private String paperName;
    private BigDecimal totalMarks;
    private Integer durationMinutes;
    private String instructions;
    private LocalDateTime generatedAt; // Mapped from AuditableEntity.createdAt
    private String generatedBy;     // Mapped from AuditableEntity.createdBy (renamed)
    private LocalDateTime updatedAt;
    private String updatedBy;

    private Set<PaperQuestionMapResponseDTO> questionMappings;
}