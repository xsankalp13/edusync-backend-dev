package com.project.edusync.teacher.model.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a teacher who is absent today and whose classes need proxy coverage.
 * Used by the admin proxy dashboard.
 */
public record AbsentStaffDto(
        UUID staffUserUuid,
        String staffName,
        String designation,
        LocalDate absentDate,
        /** Whether a proxy has already been assigned for this teacher today. */
        boolean proxyCovered
) {}
