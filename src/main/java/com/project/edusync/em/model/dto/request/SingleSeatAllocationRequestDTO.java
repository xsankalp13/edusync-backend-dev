package com.project.edusync.em.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Manual single-student seat allocation.
 * Admin picks a specific student and a specific seat.
 */
@Data
public class SingleSeatAllocationRequestDTO {

    @NotNull(message = "Exam schedule ID is required")
    private Long examScheduleId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Seat ID is required")
    private Long seatId;
}
