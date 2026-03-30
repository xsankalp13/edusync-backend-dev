package com.project.edusync.adm.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequestDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String code,
        Integer totalFloors
) {
}

