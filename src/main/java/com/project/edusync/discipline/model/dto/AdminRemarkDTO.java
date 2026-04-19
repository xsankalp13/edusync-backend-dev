package com.project.edusync.discipline.model.dto;

import com.project.edusync.discipline.model.enums.RemarkTag;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full-context remark view for the admin dashboard.
 */
@Data
@Builder
public class AdminRemarkDTO {
    private UUID uuid;
    private String message;
    private RemarkTag tag;
    private LocalDate remarkDate;
    private LocalDateTime createdAt;

    // Teacher info
    private String teacherName;
    private UUID teacherUuid;

    // Student info
    private String studentName;
    private UUID studentUuid;
    private String enrollmentNumber;

    // Class/Section info
    private String className;
    private String sectionName;
}
