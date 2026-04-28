package com.project.edusync.hrms.dto.designation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for bulk-assigning staff members to a staff designation.
 *
 * Each entry in {@code staffRefs} may be either:
 * <ul>
 *   <li>A staff UUID (preferred)</li>
 *   <li>A staff employeeId (fallback)</li>
 * </ul>
 */
public record BulkDesignationAssignRequestDTO(
        @NotEmpty(message = "staffRefs must not be empty")
        @Size(max = 500, message = "Cannot assign more than 500 staff at once")
        List<String> staffRefs
) {
}
