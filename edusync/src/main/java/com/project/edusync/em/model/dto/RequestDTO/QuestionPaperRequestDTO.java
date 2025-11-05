package com.project.edusync.em.model.dto.RequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO for creating a new QuestionPaper and its mappings all at once.
 * 'generatedBy' will be set from the security context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPaperRequestDTO {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotBlank(message = "Paper name is required")
    @Size(max = 255)
    private String paperName;

    @NotNull(message = "Total marks are required")
    @DecimalMin(value = "1.0")
    private BigDecimal totalMarks;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String instructions;

    @Valid // Validates the nested DTOs
    @NotEmpty(message = "A question paper must contain at least one question")
    private Set<PaperQuestionMapRequestDTO> questionMappings;
}