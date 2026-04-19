package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.OnboardingTemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTemplateTaskRepository extends JpaRepository<OnboardingTemplateTask, Long> {
    List<OnboardingTemplateTask> findByTemplate_IdAndActiveTrueOrderByTaskOrderAsc(Long templateId);
}

