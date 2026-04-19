package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.OnboardingTaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingTaskRecordRepository extends JpaRepository<OnboardingTaskRecord, Long> {
    List<OnboardingTaskRecord> findByRecord_IdOrderByTemplateTask_TaskOrderAsc(Long recordId);
    Optional<OnboardingTaskRecord> findByRecord_IdAndId(Long recordId, Long taskRecordId);
    long countByRecord_Id(Long recordId);
    long countByRecord_IdAndStatus(Long recordId, com.project.edusync.hrms.model.enums.TaskRecordStatus status);
}

