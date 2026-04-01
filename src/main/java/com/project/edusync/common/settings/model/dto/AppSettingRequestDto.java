package com.project.edusync.common.settings.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppSettingRequestDto(
        @NotBlank @Size(max = 150) String key,
        @NotBlank @Size(max = 10000) String value
) {
}

