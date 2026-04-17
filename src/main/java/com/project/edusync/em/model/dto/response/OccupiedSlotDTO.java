package com.project.edusync.em.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single occupied slot within a seat.
 * Used in SeatAvailabilityDTO to give the frontend rich per-position info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OccupiedSlotDTO {
    /** 0-based index: 0=LEFT, 1=MIDDLE, 2=RIGHT */
    private int positionIndex;
    /** Human-readable label derived from positionIndex */
    private String positionLabel;
    private String subjectName;
    private String className;
    private String studentName;
}
