package com.project.edusync.adm.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TimetableOverviewResponseDto {
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
    private String scheduleStatus;
    private Long totalPeriods;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUpdatedAt;
}

