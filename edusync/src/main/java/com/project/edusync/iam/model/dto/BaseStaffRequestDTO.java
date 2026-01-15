package com.project.edusync.iam.model.dto;

import com.project.edusync.uis.model.enums.StaffType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseStaffRequestDTO extends CreateUserRequestDTO {

    @NotNull(message = "Job Title is required")
    private String jobTitle;

    @NotNull(message = "Hire Date is required")
    private LocalDate hireDate;

    private String officeLocation;

    // Helper method to get the specific type in the service
    public abstract StaffType getStaffType();
}