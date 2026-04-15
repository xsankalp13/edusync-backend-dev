package com.project.edusync.hrms.dto.document;

import com.project.edusync.hrms.model.enums.DocumentCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StaffDocumentResponseDTO(
        UUID uuid,
        UUID staffRef,
        DocumentCategory category,
        String displayName,
        String originalFileName,
        String storageUrl,
        String contentType,
        Long sizeBytes,
        LocalDateTime uploadedAt,
        LocalDate expiryDate
) {}

