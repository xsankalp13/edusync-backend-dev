package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.StaffSensitiveInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffSensitiveInfoRepository extends JpaRepository<StaffSensitiveInfo, Long> {
    Optional<StaffSensitiveInfo> findByStaff_Id(Long staffId);
}

