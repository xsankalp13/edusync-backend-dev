package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.StaffType;
import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

import com.project.edusync.uis.model.enums.Department;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseStaffRequestDTO extends CreateUserRequestDTO {

    @NotNull(message = "Job Title is required")
    private String jobTitle;

    @NotNull(message = "Hire Date is required")
    private LocalDate hireDate;

    @NotNull(message = "Staff category is required")
    private StaffCategory category;

    @NotNull(message = "Department is required")
    private Department department;

    private Long designationId;

    private String officeLocation;

    // Helper method to get the specific type in the service
    public abstract StaffType getStaffType();
}