package com.project.edusync.hrms.dto.document;

import com.project.edusync.hrms.model.enums.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DocumentUploadConfirmRequestDTO {
    @NotBlank private String objectKey;
    @NotBlank private String storageUrl;
    private String contentType;
    private Long sizeBytes;
    @NotNull private DocumentCategory category;
    @NotBlank private String displayName;
    private String originalFileName;
    private LocalDate expiryDate;
}

