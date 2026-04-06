package com.project.edusync.hrms.dto.salary;

import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SalaryTemplateUpdateDTO(
        @NotBlank @Size(max = 150) String templateName,
        @Size(max = 500) String description,
        String gradeRef,
        @NotBlank String academicYear,
        StaffCategory applicableCategory,
        @NotEmpty List<@Valid TemplateComponentInputDTO> components
) {
}

