package com.project.edusync.superadmin.audit.service;

import com.project.edusync.superadmin.audit.model.dto.AuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;

public interface AuditLogService {

    void logAsync(String action,
                  String entityType,
                  String entityId,
                  String entityDisplayName,
                  Map<String, Object> changePayload,
                  String ipAddress,
                  String userAgent,
                  String actorUsernameHint);

    Page<AuditLogResponseDto> search(String actor,
                                     String action,
                                     String entityType,
                                     Instant from,
                                     Instant to,
                                     Pageable pageable);
}


