package com.project.edusync.em.model.dto.RequestDTO;

import com.project.edusync.em.model.enums.StudentAttendanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for a single student's mark entry.
 * Intended to be used inside a bulk request list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMarkRequestDTO {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    // Optional (e.g., if absent), but must be >= 0 if present
    @DecimalMin(value = "0.0", message = "Marks cannot be negative")
    private BigDecimal marksObtained;

    @NotNull(message = "Attendance status is required")
    private StudentAttendanceStatus attendanceStatus;

    private String remarks;
}