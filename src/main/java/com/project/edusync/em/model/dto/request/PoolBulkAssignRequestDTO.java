package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PoolBulkAssignRequestDTO {
    @NotNull
    private UUID examUuid;

    @NotEmpty
    private List<UUID> selectedRoomUuids;

    @NotEmpty
    private List<UUID> poolStaffUuids;
}
