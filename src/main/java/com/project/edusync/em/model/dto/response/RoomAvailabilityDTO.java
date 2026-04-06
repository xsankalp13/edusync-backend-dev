package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Room availability summary for a given time window.
 * Returned by the "get available rooms" endpoint.
 *
 * BENCH SHARING: totalCapacity = totalSeats × maxStudentsPerSeat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityDTO {

    private Long roomId;
    private UUID roomUuid;
    private String roomName;

    /** Physical seat count in the room */
    private int totalSeats;

    /** totalSeats × maxStudentsPerSeat */
    private int totalCapacity;

    /** Number of allocations currently active in the time window */
    private int occupiedCapacity;

    /** totalCapacity - occupiedCapacity */
    private int availableCapacity;

    private boolean isFull;

    /** How many students each seat can hold for this schedule */
    private int maxStudentsPerSeat;

    private int totalStudentsToSeat;

    private Integer floorNumber;

    private String mode; // "SINGLE", "DOUBLE", "SHARED"

    private java.util.List<OccupiedByDTO> occupiedBy;

    // ── Backward-compatible aliases ────────────────────────────────
    /** @deprecated Use {@link #occupiedCapacity} */
    public int getOccupiedSeats() { return occupiedCapacity; }
    /** @deprecated Use {@link #availableCapacity} */
    public int getAvailableSeats() { return availableCapacity; }
}
