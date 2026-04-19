package com.project.edusync.discipline.model.dto;

import com.project.edusync.discipline.model.enums.RemarkTag;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Simplified remark view for the student dashboard.
 */
@Data
@Builder
public class StudentRemarkDTO {
    private UUID uuid;
    private String message;
    private RemarkTag tag;
    private LocalDate remarkDate;
    private String teacherName;
}
