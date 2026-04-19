package com.project.edusync.em.model.enums;

/**
 * @deprecated Replaced by integer positionIndex on SeatAllocation.
 * Kept only for migration/backfill compatibility.
 * 0 = LEFT, 1 = MIDDLE, 2 = RIGHT
 */
@Deprecated
public enum SeatPosition {
    LEFT, RIGHT, SINGLE
}
