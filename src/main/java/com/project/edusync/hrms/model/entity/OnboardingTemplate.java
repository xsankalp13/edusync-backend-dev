package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_onboarding_templates")
@Getter
@Setter
@NoArgsConstructor
public class OnboardingTemplate extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

