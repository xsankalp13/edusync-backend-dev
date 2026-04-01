package com.project.edusync.common.settings.model.dto;

import java.util.List;

public record AppSettingBulkUpsertRequestDto(
        int saved,
        boolean restartRequired,
        List<String> restartRequiredFor
) {
}

