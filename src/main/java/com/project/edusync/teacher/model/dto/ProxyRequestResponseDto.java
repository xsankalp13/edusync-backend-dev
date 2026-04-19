package com.project.edusync.teacher.model.dto;

import com.project.edusync.teacher.model.enums.ProxyRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enriched proxy request response — includes resolved names and full status.
 * Returned by all teacher and admin proxy endpoints.
 */
public record ProxyRequestResponseDto(

        Long id,
        UUID uuid,

        // Requester (the absent / requesting teacher)
        UUID requestedByUuid,
        String requestedByName,

        // Target (the teacher asked to cover)
        UUID requestedToUuid,
        String requestedToName,

        String subject,
        LocalDate periodDate,
        UUID sectionUuid,

        ProxyRequestStatus status,
        /** Convenience alias: true iff status == ACCEPTED. */
        boolean isAccepted,

        String declineReason,
        LocalDateTime createdAt
) {}
