package com.project.edusync.hrms.dto.leavetemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BulkAssignByDesignationDTO(
        @NotBlank String designationRef,
        @NotBlank @Size(max = 20) String academicYear
) {
}
