package com.project.edusync.em.model.dto.RequestDTO;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a new ExamSchedule.
 * The parent Exam UUID will be part of the URL path (e.g., /api/exams/{uuid}/schedules).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleRequestDTO {

    @NotNull(message = "Class ID is required")
    private Long classId;

    private Long sectionId; // Optional

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Exam date is required")
    @FutureOrPresent(message = "Exam date must be in the present or future")
    private LocalDate examDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Max marks are required")
    @DecimalMin(value = "1.0", message = "Max marks must be at least 1")
    private BigDecimal maxMarks;

    @NotNull(message = "Passing marks are required")
    @DecimalMin(value = "0.0", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    @Size(max = 50)
    private String roomNumber;
}