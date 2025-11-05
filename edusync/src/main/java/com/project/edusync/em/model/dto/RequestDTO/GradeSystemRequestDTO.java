package com.project.edusync.em.model.dto.RequestDTO;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

/**
 * DTO for creating a new GradeSystem and its scales all at once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSystemRequestDTO {

    @NotBlank(message = "System name cannot be blank")
    @Size(max = 100)
    private String systemName;

    private String description;

    // If null, service should default to true
    private Boolean isActive;

    @Valid // Validates the nested DTOs
    @NotEmpty(message = "A grade system must have at least one scale")
    private Set<GradeScaleRequestDTO> gradeScales;
}