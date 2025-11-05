package com.project.edusync.em.model.dto.ResponseDTO;
import com.project.edusync.em.model.enums.PastExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for sending PastPaper information back to the client.
 * This includes the public UUID and the full file URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastPaperResponseDTO {

    private UUID uuid;
    private String title;
    private Long classId;
    private Long subjectId;
    private Integer examYear;
    private PastExamType examType;
    private String fileUrl;
    private String fileMimeType;
    private Integer fileSizeKb;
    private LocalDateTime uploadedAt; // Mapped from AuditableEntity.createdAt
    private String uploadedBy;     // Mapped from AuditableEntity.createdBy (renamed)
    private LocalDateTime updatedAt;
    private String updatedBy;
}
