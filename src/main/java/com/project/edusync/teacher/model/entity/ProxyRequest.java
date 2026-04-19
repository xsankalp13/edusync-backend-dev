package com.project.edusync.teacher.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.teacher.model.enums.ProxyRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a peer proxy (substitute) request from one teacher to another
 * for a specific class period on a given date.
 *
 * <p>The {@code status} field is the single source of truth for lifecycle state.
 * {@code isAccepted} is kept as a derived column for backward compatibility
 * with existing queries — it is always kept in sync with status.</p>
 */
@Entity
@Table(name = "proxy_requests")
@Getter
@Setter
@NoArgsConstructor
public class ProxyRequest extends AuditableEntity {

    // id, uuid, createdAt, updatedAt, createdBy, updatedBy inherited from AuditableEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_to", nullable = false)
    private User requestedTo;

    @Column(nullable = false, length = 200)
    private String subject;

    /** Date the proxy class is required on. */
    @Column(name = "period_date")
    private LocalDate periodDate;

    /** UUID of the section that needs to be covered (nullable for legacy rows). */
    @Column(name = "section_uuid")
    private UUID sectionUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProxyRequestStatus status = ProxyRequestStatus.PENDING;

    /** Optional reason provided when declining a request. */
    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    /**
     * Kept for backward compatibility — always derived from {@code status}.
     * Updated automatically via {@link #syncIsAccepted()}.
     */
    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted = false;

    /** Syncs the legacy isAccepted flag from the status enum. Call after every status change. */
    public void syncIsAccepted() {
        this.isAccepted = (this.status == ProxyRequestStatus.ACCEPTED);
    }
}
