package com.project.edusync.common.settings.repository;

import com.project.edusync.common.settings.model.entity.AppSetting;
import com.project.edusync.common.settings.model.enums.SettingGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {

    List<AppSetting> findAllBySettingGroupOrderByKeyAsc(SettingGroup settingGroup);

    List<AppSetting> findAllByOrderBySettingGroupAscKeyAsc();

    List<AppSetting> findByKeyStartingWithOrderByKeyAsc(String keyPrefix);
}

