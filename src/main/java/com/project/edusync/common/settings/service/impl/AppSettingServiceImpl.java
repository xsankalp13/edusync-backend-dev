package com.project.edusync.common.settings.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.settings.model.dto.AppSettingBulkUpsertRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingResponseDto;
import com.project.edusync.common.settings.model.dto.PublicWhitelabelSettingsResponseDto;
import com.project.edusync.common.settings.model.entity.AppSetting;
import com.project.edusync.common.settings.model.enums.SettingGroup;
import com.project.edusync.common.settings.model.enums.SettingType;
import com.project.edusync.common.settings.repository.AppSettingRepository;
import com.project.edusync.common.settings.security.AppSettingCryptoService;
import com.project.edusync.common.settings.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppSettingServiceImpl implements AppSettingService {

    private static final String MASKED_SECRET = "********";
    private static final String PUBLIC_CACHE_KEY = "public-whitelabel";

    private final AppSettingRepository appSettingRepository;
    private final AppSettingCryptoService appSettingCryptoService;

    private final Cache<String, PublicWhitelabelSettingsResponseDto> publicWhitelabelCache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<AppSettingResponseDto>> getSettings(SettingGroup group) {
        List<AppSetting> settings = group == null
                ? appSettingRepository.findAllByOrderBySettingGroupAscKeyAsc()
                : appSettingRepository.findAllBySettingGroupOrderByKeyAsc(group);

        Map<String, List<AppSettingResponseDto>> grouped = new LinkedHashMap<>();
        for (AppSetting setting : settings) {
            String groupName = setting.getSettingGroup().name();
            grouped.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(toResponse(setting));
        }
        return grouped;
    }

    @Override
    @Transactional
    public AppSettingBulkUpsertRequestDto patchSettings(List<AppSettingRequestDto> requestDtos) {
        if (requestDtos == null || requestDtos.isEmpty()) {
            throw new EdusyncException("At least one setting update is required.", HttpStatus.BAD_REQUEST);
        }

        List<String> restartRequiredFor = new ArrayList<>();
        String actor = currentActor();
        Instant now = Instant.now();

        for (AppSettingRequestDto request : requestDtos) {
            AppSetting setting = appSettingRepository.findById(request.key())
                    .orElseThrow(() -> new EdusyncException("Setting not found: " + request.key(), HttpStatus.NOT_FOUND));

            String normalizedValue = normalizeByType(setting.getType(), request.value(), setting.getKey());
            if (setting.getType() == SettingType.ENCRYPTED) {
                setting.setValue(appSettingCryptoService.encryptForStorage(normalizedValue));
            } else {
                setting.setValue(normalizedValue);
            }

            setting.setUpdatedAt(now);
            setting.setUpdatedBy(actor);
            appSettingRepository.save(setting);

            if (setting.isRequiresRestart()) {
                restartRequiredFor.add(setting.getKey());
            }
        }

        invalidatePublicCachesIfNeeded(requestDtos);
        return new AppSettingBulkUpsertRequestDto(
                requestDtos.size(),
                !restartRequiredFor.isEmpty(),
                restartRequiredFor
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PublicWhitelabelSettingsResponseDto getPublicWhitelabelSettings() {
        PublicWhitelabelSettingsResponseDto cached = publicWhitelabelCache.getIfPresent(PUBLIC_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        Map<String, AppSetting> byKey = new LinkedHashMap<>();
        for (AppSetting setting : appSettingRepository.findAll()) {
            byKey.put(setting.getKey(), setting);
        }

        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("finance", readBoolean(byKey, "feature.finance", true));
        features.put("examination", readBoolean(byKey, "feature.examination", true));
        features.put("attendance", readBoolean(byKey, "feature.attendance", true));
        features.put("timetableAi", readBoolean(byKey, "feature.timetable_ai", true));
        features.put("bulkImport", readBoolean(byKey, "feature.bulk_import", true));
        features.put("parentPortal", readBoolean(byKey, "feature.parent_portal", false));
        features.put("smsNotifications", readBoolean(byKey, "feature.sms_notifications", false));

        PublicWhitelabelSettingsResponseDto payload = new PublicWhitelabelSettingsResponseDto(
                readString(byKey, "school.name", "My School"),
                readString(byKey, "school.logo_url", ""),
                readString(byKey, "school.primary_color", "#6366f1"),
                readString(byKey, "school.accent_color", "#8b5cf6"),
                readString(byKey, "school.timezone", "Asia/Kolkata"),
                readString(byKey, "school.currency", "INR"),
                readString(byKey, "school.date_format", "DD/MM/YYYY"),

                readString(byKey, "school.short_name", ""),
                readString(byKey, "school.tagline", ""),
                readString(byKey, "school.address", ""),
                readString(byKey, "school.phone", ""),
                readString(byKey, "school.email", ""),
                readString(byKey, "school.id_card_header_mode", "TEXT"),
                readString(byKey, "school.id_card_header_image_url", ""),

                features
        );

        publicWhitelabelCache.put(PUBLIC_CACHE_KEY, payload);
        return payload;
    }

    @Override
    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        return appSettingRepository.findById(key)
                .map(setting -> setting.getType() == SettingType.ENCRYPTED
                        ? appSettingCryptoService.decryptFromStorage(setting.getValue())
                        : setting.getValue())
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getValue(key, String.valueOf(defaultValue)));
    }

    private AppSettingResponseDto toResponse(AppSetting setting) {
        String exposedValue;
        if (setting.isSensitive()) {
            exposedValue = MASKED_SECRET;
        } else if (setting.getType() == SettingType.ENCRYPTED) {
            exposedValue = MASKED_SECRET;
        } else {
            exposedValue = setting.getValue();
        }

        return new AppSettingResponseDto(
                setting.getKey(),
                exposedValue,
                setting.getType(),
                setting.isSensitive(),
                setting.isRequiresRestart(),
                setting.getDescription()
        );
    }

    private String normalizeByType(SettingType type, String value, String key) {
        String normalized = value == null ? "" : value.trim();
        return switch (type) {
            case BOOLEAN -> normalizeBoolean(normalized, key);
            case INTEGER -> normalizeInteger(normalized, key);
            case STRING, ENCRYPTED, JSON -> normalized;
        };
    }

    private String normalizeBoolean(String value, String key) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (!"true".equals(lower) && !"false".equals(lower)) {
            throw new EdusyncException("Invalid boolean value for setting: " + key, HttpStatus.BAD_REQUEST);
        }
        return lower;
    }

    private String normalizeInteger(String value, String key) {
        try {
            return String.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            throw new EdusyncException("Invalid integer value for setting: " + key, HttpStatus.BAD_REQUEST);
        }
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "system";
        }
        return auth.getName();
    }

    private String readString(Map<String, AppSetting> byKey, String key, String fallback) {
        AppSetting setting = byKey.get(key);
        if (setting == null) {
            return fallback;
        }
        if (setting.getType() == SettingType.ENCRYPTED) {
            return appSettingCryptoService.decryptFromStorage(setting.getValue());
        }
        return setting.getValue();
    }

    private boolean readBoolean(Map<String, AppSetting> byKey, String key, boolean fallback) {
        return Boolean.parseBoolean(readString(byKey, key, String.valueOf(fallback)));
    }

    private void invalidatePublicCachesIfNeeded(List<AppSettingRequestDto> requestDtos) {
        boolean touchedPublic = requestDtos.stream().anyMatch(request ->
                request.key().startsWith("feature.") || request.key().startsWith("school."));
        if (touchedPublic) {
            publicWhitelabelCache.invalidate(PUBLIC_CACHE_KEY);
        }
    }
}

