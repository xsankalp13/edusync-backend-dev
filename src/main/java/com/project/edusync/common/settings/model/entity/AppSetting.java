package com.project.edusync.common.settings.model.entity;

import com.project.edusync.common.settings.model.enums.SettingGroup;
import com.project.edusync.common.settings.model.enums.SettingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 150)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettingGroup settingGroup;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean requiresRestart;

    @Column(nullable = false)
    private boolean sensitive;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 100)
    private String updatedBy;
}

