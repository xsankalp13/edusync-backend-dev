package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "hrms_course_certificates")
@Getter
@Setter
@NoArgsConstructor
public class CourseCertificate extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    private CourseEnrollment enrollment;

    @Column(name = "cert_title", length = 300)
    private String certTitle;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "object_key", length = 1000)
    private String objectKey;

    @Column(name = "storage_url", length = 2000)
    private String storageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

