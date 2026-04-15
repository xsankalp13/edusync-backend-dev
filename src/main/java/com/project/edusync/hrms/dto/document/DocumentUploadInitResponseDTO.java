package com.project.edusync.hrms.dto.document;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class DocumentUploadInitResponseDTO {
    private String provider;
    private String method;
    private String uploadUrl;
    private String objectKey;
    private Instant expiresAt;
    private Map<String, String> fields;
    private Map<String, String> headers;
}

