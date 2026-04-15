package com.project.edusync.admission.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SectionSaveRequest<T> {
    @NotNull(message = "Section number is required")
    @Min(1) @Max(9)
    private int sectionNumber;

    @NotNull(message = "Section data is required")
    private T data;
}
