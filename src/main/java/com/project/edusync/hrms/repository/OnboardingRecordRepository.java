package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.OnboardingRecord;
import com.project.edusync.hrms.model.enums.OnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingRecordRepository extends JpaRepository<OnboardingRecord, Long> {
    Optional<OnboardingRecord> findByUuid(UUID uuid);
    List<OnboardingRecord> findByStaff_Id(Long staffId);
    List<OnboardingRecord> findByStaff_IdAndStatus(Long staffId, OnboardingStatus status);
    List<OnboardingRecord> findByStatus(OnboardingStatus status);
    List<OnboardingRecord> findAllByActiveTrue();
}

