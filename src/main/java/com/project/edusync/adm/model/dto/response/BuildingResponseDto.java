package com.project.edusync.adm.model.dto.response;

import java.util.UUID;

public record BuildingResponseDto(
        UUID uuid,
        String name,
        String code,
        Integer totalFloors
) {
}

