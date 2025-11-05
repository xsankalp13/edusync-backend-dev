package com.project.edusync.finance.dto.feetype;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeeTypeCreateUpdateDTO {

    @NotNull
    @Size(min = 3, max = 50)
    private String typeName;

    @Size(max = 255)
    private String description;
}