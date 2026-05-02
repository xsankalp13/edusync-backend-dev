package com.project.edusync.finance.dto.studentfee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// Used for responses (e.g., after creation)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeMapResponseDTO {
    private Long mapId;
    private Long studentId;
    private String studentName;
    private Long structureId;
    private LocalDate effectiveDate;
    private String notes;
    // Getters and Setters
}