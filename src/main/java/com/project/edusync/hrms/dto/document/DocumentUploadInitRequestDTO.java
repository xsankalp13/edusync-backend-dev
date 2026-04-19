package com.project.edusync.hrms.dto.document;

import com.project.edusync.hrms.model.enums.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentUploadInitRequestDTO {
    @NotBlank private String fileName;
    @NotBlank private String contentType;
    private long sizeBytes;
    @NotNull private DocumentCategory category;
    @NotBlank private String displayName;
    private LocalDate expiryDate;
}

