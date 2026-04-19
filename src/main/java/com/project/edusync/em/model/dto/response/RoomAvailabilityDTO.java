package com.project.edusync.em.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Room availability summary for a given time window.
 * Returned by the "get available rooms" endpoint.
 *
 * DYNAMIC SEATING: totalCapacity = totalSeats × maxStudentsPerSeat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /** "SINGLE" (1), "DOUBLE" (2), "TRIPLE" (3) */
    private String mode;

    private java.util.List<OccupiedByDTO> occupiedBy;

    // ── Backward-compatible aliases ────────────────────────────────
    /** @deprecated Use {@link #occupiedCapacity} */
    @JsonIgnore
    public int getOccupiedSeats() { return occupiedCapacity; }
    /** @deprecated Use {@link #availableCapacity} */
    @JsonIgnore
    public int getAvailableSeats() { return availableCapacity; }
}
