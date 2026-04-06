package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual seat availability within a room for a given time window.
 * Frontend can render this as a visual seat grid with occupancy indicators.
 *
 * BENCH SHARING: Each seat now has capacity/occupiedCount/availableSlots
 * instead of a simple boolean available flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityDTO {

    private Long seatId;
    private String label;
    private int rowNumber;
    private int columnNumber;

    /** Max students this seat can hold (from schedule.maxStudentsPerSeat) */
    private int capacity;

    /** Current allocation count in the time window */
    private int occupiedCount;

    /** capacity - occupiedCount */
    private int availableSlots;

    /** True if occupiedCount >= capacity or blocked by single-seating */
    private boolean isFull;

    /** Backward compatible: true if seat has any capacity left */
    private boolean available;

    /** Name of the occupying student (only set for single-seat mode) */
    private String occupiedByStudentName;

    /** List of occupied position names, e.g. ["LEFT"] */
    private java.util.List<String> occupiedPositions;
}
