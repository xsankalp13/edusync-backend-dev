package com.project.edusync.em.model.dto.ResponseDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmitCardGenerationProgressDTO {
    private int total;
    private int generated;
    private int published;
    private int failed;
    private int percentage;
}

