package com.project.edusync.teacher.model.dto;

/**
 * Optional payload when declining a peer proxy request.
 */
public record ProxyDeclineDto(
        /** Optional human-readable reason for declining. */
        String reason
) {}
