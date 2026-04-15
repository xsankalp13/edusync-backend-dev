package com.project.edusync.hrms.dto.leavetemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffLeaveTemplateMappingRequestDTO(
        @NotBlank String staffRef,
        @NotBlank @Size(max = 20) String academicYear
) {
}
