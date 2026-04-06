package com.project.edusync.superadmin.audit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.superadmin.audit.model.dto.AuditLogResponseDto;
import com.project.edusync.superadmin.audit.model.entity.AuditLog;
import com.project.edusync.superadmin.audit.repository.AuditLogRepository;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final BlockingQueue<AuditLog> auditLogQueue;
    private final ObjectMapper objectMapper;

    @Override
    public void logAsync(String action,
                         String entityType,
                         String entityId,
                         String entityDisplayName,
                         Map<String, Object> changePayload,
                         String ipAddress,
                         String userAgent,
                         String actorUsernameHint) {
        try {
            ActorInfo actorInfo = resolveActor(actorUsernameHint);

            AuditLog logEntry = new AuditLog();
            logEntry.setActorUsername(actorInfo.username());
            logEntry.setActorRole(actorInfo.role());
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            logEntry.setEntityDisplayName(entityDisplayName);
            logEntry.setChangePayload(serializePayload(changePayload));
            logEntry.setIpAddress(ipAddress);
            logEntry.setUserAgent(userAgent);
            logEntry.setTimestamp(Instant.now());

            boolean accepted = auditLogQueue.offer(logEntry);
            if (!accepted) {
                log.warn("Audit queue at capacity - dropping event: action={}, actor={}", action, actorInfo.username());
            }
        } catch (Exception ex) {
            log.error("Failed to write audit log action={} entityType={} entityId={}", action, entityType, entityId, ex);
        }
    }

    private ActorInfo resolveActor(String actorUsernameHint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAuthenticatedContext = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (hasAuthenticatedContext) {
            String actorUsername = authentication.getName();
            String actorRole = authentication.getAuthorities().stream()
                    .map(granted -> granted.getAuthority())
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("SYSTEM");
            if (actorRole.startsWith("ROLE_")) {
                actorRole = actorRole.substring(5);
            }
            if (StringUtils.hasText(actorUsername)) {
                return new ActorInfo(actorUsername, actorRole);
            }
        }

        if (StringUtils.hasText(actorUsernameHint)) {
            return new ActorInfo(actorUsernameHint.trim(), "USER");
        }

        return new ActorInfo("system", "SYSTEM");
    }

    private record ActorInfo(String username, String role) {}

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> search(String actor,
                                            String action,
                                            String entityType,
                                            Instant from,
                                            Instant to,
                                            Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(actor)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("actorUsername")), "%" + actor.trim().toLowerCase() + "%"));
        }
        if (StringUtils.hasText(action)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (StringUtils.hasText(entityType)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType.trim()));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private String serializePayload(Map<String, Object> payload) {
        Map<String, Object> source = payload == null ? Map.of("before", Map.of(), "after", Map.of()) : sanitize(payload);
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private JsonNode parsePayload(String raw) {
        try {
            return objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private AuditLogResponseDto toResponse(AuditLog logEntry) {
        return new AuditLogResponseDto(
                logEntry.getId(),
                logEntry.getActorUsername(),
                logEntry.getActorRole(),
                logEntry.getAction(),
                logEntry.getEntityType(),
                logEntry.getEntityId(),
                logEntry.getEntityDisplayName(),
                parsePayload(logEntry.getChangePayload()),
                logEntry.getIpAddress(),
                logEntry.getTimestamp()
        );
    }

    private Map<String, Object> sanitize(Map<String, Object> payload) {
        Map<String, Object> safe = new HashMap<>(payload);
        maskSensitive(safe, "password");
        maskSensitive(safe, "token");
        maskSensitive(safe, "secret");
        return safe;
    }

    private void maskSensitive(Map<String, Object> map, String token) {
        map.replaceAll((key, value) -> {
            if (key != null && key.toLowerCase().contains(token)) {
                return "***";
            }
            return value;
        });
    }
}





