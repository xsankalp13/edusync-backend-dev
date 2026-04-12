package com.project.edusync.em.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmitCardPdfJobPayload {
    @Builder.Default
    private String jobId = UUID.randomUUID().toString();
    private Long examId;
    private Long studentId;
    private Long admitCardId;
    @Builder.Default
    private int retryCount = 0;
}

