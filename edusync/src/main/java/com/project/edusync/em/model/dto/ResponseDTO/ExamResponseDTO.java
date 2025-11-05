package com.project.edusync.em.model.dto.ResponseDTO;


import com.project.edusync.em.model.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for sending Exam data to the client.
 * Includes the public UUID and all audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponseDTO {

    private UUID uuid;
    private String name;
    private String academicYear;
    private ExamType examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;


}
