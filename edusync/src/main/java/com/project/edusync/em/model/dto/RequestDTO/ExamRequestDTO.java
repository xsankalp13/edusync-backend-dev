package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


/**
 * DTO for creating or updating an Exam.
 * 'createdBy' and 'updatedBy' will be handled by the system context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRequestDTO {

    @NotBlank(message = "Exam name cannot be blank")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Academic year cannot be blank")
    @Size(max = 10, message = "Academic year format is YYYY-YY (e.g., 2025-26)")
    private String academicYear;

    @NotNull(message = "Exam type must be specified")
    private ExamType examType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;


    // Note: isPublished is not here. It should be updated via a separate
    // "publish" endpoint, not during creation.
}
