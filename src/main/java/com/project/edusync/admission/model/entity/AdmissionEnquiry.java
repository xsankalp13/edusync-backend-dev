package com.project.edusync.admission.model.entity;

import com.project.edusync.admission.model.enums.EnquiryStatus;
import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.iam.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission_enquiries")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionEnquiry extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private String adminRepliedBy;

    private LocalDateTime adminRepliedAt;

    private String classApplyingFor;

    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EnquiryStatus status = EnquiryStatus.PENDING;
}
