package com.project.edusync.em.model.dto.RequestDTO;
import com.project.edusync.em.model.enums.DifficultyLevel;
import com.project.edusync.em.model.enums.QuestionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for creating a new question in the QuestionBank.
 * 'createdBy' will be set automatically from the security context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankRequestDTO {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @Size(max = 255)
    private String topic;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficultyLevel;

    @NotBlank(message = "Question text cannot be blank")
    private String questionText;

    // Optional: Service layer should validate these if type is MCQ
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer;

    @NotNull(message = "Marks are required")
    @DecimalMin(value = "0.5", message = "Marks must be at least 0.5")
    private BigDecimal marks;
}