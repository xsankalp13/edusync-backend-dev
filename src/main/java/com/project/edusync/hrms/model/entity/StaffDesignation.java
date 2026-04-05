package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hrms_staff_designations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hrms_staff_designation_code", columnNames = {"designation_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StaffDesignation extends AuditableEntity {

    @Column(name = "designation_code", nullable = false, length = 20)
    private String designationCode;

    @Column(name = "designation_name", nullable = false, length = 100)
    private String designationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private StaffCategory category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

