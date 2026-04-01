package com.project.edusync.common.settings.service;

import com.project.edusync.common.settings.model.dto.AppSettingBulkUpsertRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingResponseDto;
import com.project.edusync.common.settings.model.dto.PublicWhitelabelSettingsResponseDto;
import com.project.edusync.common.settings.model.enums.SettingGroup;

import java.util.List;
import java.util.Map;

public interface AppSettingService {

    Map<String, List<AppSettingResponseDto>> getSettings(SettingGroup group);

    AppSettingBulkUpsertRequestDto patchSettings(List<AppSettingRequestDto> requestDtos);

    PublicWhitelabelSettingsResponseDto getPublicWhitelabelSettings();

    String getValue(String key, String defaultValue);

    boolean getBooleanValue(String key, boolean defaultValue);
}

