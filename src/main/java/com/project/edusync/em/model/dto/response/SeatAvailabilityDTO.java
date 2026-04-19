package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Individual seat availability within a room for a given time window.
 * Frontend can render this as a visual seat grid with occupancy indicators.
 *
 * DYNAMIC SEATING: Each seat has capacity/occupiedCount/availableSlots
 * and detailed occupiedSlots showing who sits where.
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

    /** True if occupiedCount >= capacity */
    private boolean isFull;

    /** Backward compatible: true if seat has any capacity left */
    private boolean available;

    /** Detailed info per occupied slot: positionIndex, subject, class, student */
    private List<OccupiedSlotDTO> occupiedSlots;
}
