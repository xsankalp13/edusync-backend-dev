package com.project.edusync.hrms.dto.designation;

import java.util.List;

/**
 * Response payload returned after a bulk staff-designation assignment.
 * Provides a resilient row-by-row summary so callers can see exactly which
 * staff were updated and which failed (and why).
 */
public record BulkDesignationAssignResultDTO(
        String designationCode,
        String designationName,
        int totalRequested,
        int successCount,
        int failureCount,
        List<String> successfulEmployeeIds,
        List<String> errors
) {
}
