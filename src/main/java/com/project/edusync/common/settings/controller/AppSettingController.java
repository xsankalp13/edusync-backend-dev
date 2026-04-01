package com.project.edusync.common.settings.controller;

import com.project.edusync.common.settings.model.dto.AppSettingBulkUpsertRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingRequestDto;
import com.project.edusync.common.settings.model.dto.AppSettingResponseDto;
import com.project.edusync.common.settings.model.dto.PublicWhitelabelSettingsResponseDto;
import com.project.edusync.common.settings.model.enums.SettingGroup;
import com.project.edusync.common.settings.service.AppSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Settings", description = "Runtime app settings and public whitelabel metadata")
public class AppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping("${api.url}/super/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List app settings grouped by setting group", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, List<AppSettingResponseDto>>> getSettings(
            @RequestParam(value = "group", required = false) SettingGroup group) {
        return ResponseEntity.ok(appSettingService.getSettings(group));
    }

    @PatchMapping("${api.url}/super/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk update app settings", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AppSettingBulkUpsertRequestDto> patchSettings(
            @Valid @RequestBody List<@Valid AppSettingRequestDto> request) {
        return ResponseEntity.ok(appSettingService.patchSettings(request));
    }

    @GetMapping("${api.url}/public/settings/whitelabel")
    @Operation(summary = "Public whitelabel + feature flags for frontend boot")
    public ResponseEntity<PublicWhitelabelSettingsResponseDto> getPublicWhitelabelSettings() {
        return ResponseEntity.ok(appSettingService.getPublicWhitelabelSettings());
    }
}


