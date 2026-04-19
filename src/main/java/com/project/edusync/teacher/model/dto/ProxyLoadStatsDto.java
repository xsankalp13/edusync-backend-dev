package com.project.edusync.teacher.model.dto;

import java.util.UUID;

/**
 * Proxy load statistics for a teacher — used by admin for equitable assignment.
 */
public record ProxyLoadStatsDto(
        UUID staffUserUuid,
        String staffName,
        int proxiesThisWeek,
        int proxiesThisMonth
) {}
