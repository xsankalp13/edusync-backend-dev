package com.project.edusync.em.model.dto.RequestDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nested DTO for mapping a question (from the bank) to a paper.
 * This is not expected to be created on its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperQuestionMapRequestDTO {

    @NotNull(message = "Question UUID is required")
    private UUID questionUuid; // Use the public UUID of the question

    @NotBlank(message = "Question number is required")
    @Size(max = 10)
    private String questionNumber;

    @NotNull(message = "Marks for this question are required")
    @DecimalMin(value = "0.5")
    private BigDecimal marksForQuestion;
}