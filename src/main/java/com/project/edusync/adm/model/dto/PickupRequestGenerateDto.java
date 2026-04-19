package com.project.edusync.adm.model.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PickupRequestGenerateDto {
    private String studentUuid;
    private Long studentId;
}
