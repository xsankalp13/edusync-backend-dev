package com.project.edusync.superadmin.audit.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record AuditLogResponseDto(
        Long id,
        String actorUsername,
        String actorRole,
        String action,
        String entityType,
        String entityId,
        String entityDisplayName,
        JsonNode changePayload,
        String ipAddress,
        Instant timestamp
) {
}

