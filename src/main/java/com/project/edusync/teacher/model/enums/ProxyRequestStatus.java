package com.project.edusync.teacher.model.enums;

/**
 * Lifecycle states for a teacher proxy request.
 *
 * <pre>
 *   PENDING  → ACCEPTED  (requestedTo accepts)
 *   PENDING  → DECLINED  (requestedTo declines)
 *   PENDING  → CANCELLED (requestedBy cancels, or admin cancels)
 *   ACCEPTED → CANCELLED (admin revokes after acceptance)
 * </pre>
 */
public enum ProxyRequestStatus {
    /** Created, awaiting acceptance by the target teacher. */
    PENDING,
    /** Target teacher has accepted — proxy is confirmed. */
    ACCEPTED,
    /** Target teacher declined the request. */
    DECLINED,
    /** Cancelled by the requester, admin, or system. */
    CANCELLED
}
