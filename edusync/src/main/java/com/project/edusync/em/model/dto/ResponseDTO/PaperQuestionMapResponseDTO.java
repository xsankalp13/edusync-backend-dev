package com.project.edusync.em.model.dto.ResponseDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nested DTO for returning a question mapping.
 * This entity is not auditable, so it returns its Long PK.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperQuestionMapResponseDTO {

    private Long paperQuestionId;
    private UUID questionUuid; // The public UUID of the question
    private String questionNumber;
    private BigDecimal marksForQuestion;

    // For convenience, the service could also populate this:
    // private QuestionBankResponseDTO question;
}