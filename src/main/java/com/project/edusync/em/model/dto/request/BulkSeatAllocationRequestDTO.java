package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Bulk auto-allocate all unassigned students from the schedule's class/section
 * into available seats in the specified room.
 */
@Data
public class BulkSeatAllocationRequestDTO {

    @NotNull(message = "Exam schedule ID is required")
    private Long examScheduleId;

    @NotNull(message = "Room ID is required")
    private UUID roomId;
}
