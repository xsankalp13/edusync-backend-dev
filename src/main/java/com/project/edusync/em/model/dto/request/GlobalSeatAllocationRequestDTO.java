package com.project.edusync.em.model.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GlobalSeatAllocationRequestDTO {
    
    @NotNull(message = "Exam UUID is required")
    private UUID examUuid;

    private java.util.List<UUID> selectedRoomUuids;
}
