package com.project.edusync.em.model.dto.RequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for submitting all marks for a schedule at once.
 * Sent to: POST /api/schedules/{scheduleId}/marks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkMarkRequestDTO {

    @NotEmpty(message = "Marks list cannot be empty")
    @Valid // This is crucial to validate each item in the list
    private List<StudentMarkRequestDTO> marks;
}