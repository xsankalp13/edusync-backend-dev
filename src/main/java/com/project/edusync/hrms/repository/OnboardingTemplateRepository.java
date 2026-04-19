package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.OnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, Long> {
    Optional<OnboardingTemplate> findByUuid(UUID uuid);
    List<OnboardingTemplate> findAllByActiveTrue();
    boolean existsByTemplateName(String templateName);
}

