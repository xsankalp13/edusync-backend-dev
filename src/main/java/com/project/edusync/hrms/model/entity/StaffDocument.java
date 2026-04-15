package com.project.edusync.hrms.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.hrms.model.enums.DocumentCategory;
import com.project.edusync.uis.model.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrms_staff_documents")
@Getter
@Setter
@NoArgsConstructor
public class StaffDocument extends AuditableEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private DocumentCategory category;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "storage_url", nullable = false, length = 2000)
    private String storageUrl;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

