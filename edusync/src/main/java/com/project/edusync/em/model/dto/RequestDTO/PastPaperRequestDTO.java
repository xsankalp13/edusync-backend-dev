package com.project.edusync.em.model.dto.RequestDTO;


import com.project.edusync.em.model.enums.PastExamType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new PastPaper metadata entry.
 * The actual file upload (MultipartFile) will be handled by the controller,
 * not this DTO. 'uploadedBy' will be set from the security context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastPaperRequestDTO {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255)
    private String title;

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Exam year is required")
    @Min(value = 1990, message = "Exam year must be valid")
    private Integer examYear;

    private PastExamType examType; // This can be optional
}