package com.project.edusync.adm.model.dto;

import com.project.edusync.adm.model.entity.PickupStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PickupRequestResponseDto {
    private String uuid;
    private String studentUuid;
    private String studentName;
    private String studentClassInfo;
    private String generatedByName;
    
    private String qrToken; // Only populated on generation, omit usually
    private PickupStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
}
