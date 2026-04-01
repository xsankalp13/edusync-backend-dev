package com.project.edusync.common.settings.model.dto;

import java.util.Map;

public record PublicWhitelabelSettingsResponseDto(
        String schoolName,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String timezone,
        String currency,
        String dateFormat,
        Map<String, Boolean> features
) {
}

