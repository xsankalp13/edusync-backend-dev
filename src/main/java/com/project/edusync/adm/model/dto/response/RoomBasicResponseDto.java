package com.project.edusync.adm.model.dto.response;

import java.util.UUID;

public record RoomBasicResponseDto(
        UUID uuid,
        String name
) {
}

