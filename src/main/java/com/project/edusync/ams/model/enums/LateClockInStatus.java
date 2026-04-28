package com.project.edusync.ams.model.enums;

public enum LateClockInStatus {
    PENDING,    // Awaiting admin review
    APPROVED,   // Admin approved — attendance upgraded to PRESENT
    REJECTED    // Admin rejected — attendance stays LATE / HALF_DAY
}
