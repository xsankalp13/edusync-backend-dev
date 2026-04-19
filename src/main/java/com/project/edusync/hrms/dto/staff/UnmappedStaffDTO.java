package com.project.edusync.hrms.dto.staff;

import lombok.Builder;

@Builder
public record UnmappedStaffDTO(
        Long staffId,
        String uuid,
        String staffName,
        String employeeId,
        String category,
        String designationName,
        String departmentName
) {}
