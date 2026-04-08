package com.project.edusync.teacher.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LectureLogUploadInitResponseDto {
    private String provider;
    private String method;
    private String uploadUrl;
    private String objectKey;
    private Instant expiresAt;
    private Map<String, String> fields;
    private Map<String, String> headers;
}
