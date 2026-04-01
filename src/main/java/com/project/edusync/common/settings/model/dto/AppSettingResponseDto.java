package com.project.edusync.common.settings.model.dto;

import com.project.edusync.common.settings.model.enums.SettingType;

public record AppSettingResponseDto(
        String key,
        String value,
        SettingType type,
        boolean sensitive,
        boolean requiresRestart,
        String description
) {
}

