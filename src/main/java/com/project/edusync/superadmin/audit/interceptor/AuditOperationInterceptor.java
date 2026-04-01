package com.project.edusync.superadmin.audit.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.utils.RequestUtil;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditOperationInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private final RequestUtil requestUtil;
    private final ObjectMapper objectMapper;
    private final AuthUtil authUtil;

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }

        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        String action = resolveAction(request, response, method);
        if (action == null) {
            return;
        }

        String entityType = resolveEntityType(handlerMethod, request.getRequestURI());
        String entityId = resolveEntityId(request);
        String entityDisplayName = handlerMethod.getMethod().getName();

        Map<String, Object> payload = buildPayload(request, response, ex);
        String actorUsernameHint = resolveActorUsernameHint(request, action);

        auditLogService.logAsync(
                action,
                entityType,
                entityId,
                entityDisplayName,
                payload,
                requestUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                actorUsernameHint
        );
    }

    private String resolveActorUsernameHint(HttpServletRequest request, String action) {
        if ("LOGIN".equals(action)) {
            return readFieldFromBody(request, "username");
        }
        if ("LOGOUT".equals(action)) {
            String refreshToken = readFieldFromBody(request, "refreshToken");
            if (StringUtils.hasText(refreshToken)) {
                try {
                    return authUtil.getUsernameFromToken(refreshToken);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String readFieldFromBody(HttpServletRequest request, String fieldName) {
        if (!(request instanceof ContentCachingRequestWrapper wrappedRequest)) {
            return null;
        }

        byte[] body = wrappedRequest.getContentAsByteArray();
        if (body.length == 0) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            JsonNode field = root.get(fieldName);
            return field == null || field.isNull() ? null : field.asText(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveAction(HttpServletRequest request, HttpServletResponse response, String method) {
        String path = request.getRequestURI().toLowerCase();

        if (path.contains("/auth/login")) {
            return "LOGIN";
        }
        if (path.contains("/auth/logout")) {
            return "LOGOUT";
        }
        if (path.contains("/refresh-token")) {
            return null;
        }
        if (path.contains("/reset-password") || path.contains("/forgot-password")) {
            return "PASSWORD_RESET";
        }
        if (path.contains("/force-logout") || path.contains("/sessions/invalidate-all")) {
            return "FORCE_LOGOUT";
        }
        if (path.contains("/bulk-import")) {
            return response.getStatus() >= 400 ? "BULK_IMPORT_FAILED" : "BULK_IMPORT_COMPLETED";
        }
        if (path.contains("/rbac/roles/") && path.contains("/permissions/")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "PERMISSION_ASSIGNED";
            }
            if ("DELETE".equalsIgnoreCase(method)) {
                return "PERMISSION_REVOKED";
            }
        }
        if (path.contains("/super/settings") && ("PATCH".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
            return "CONFIG_CHANGED";
        }
        if (path.contains("/activation")) {
            String active = request.getParameter("active");
            if ("true".equalsIgnoreCase(active)) {
                return "ACTIVATED";
            }
            if ("false".equalsIgnoreCase(active)) {
                return "DEACTIVATED";
            }
        }

        return switch (method.toUpperCase()) {
            case "POST" -> "CREATED";
            case "PUT", "PATCH" -> "UPDATED";
            case "DELETE" -> "DELETED";
            default -> null;
        };
    }

    private String resolveEntityType(HandlerMethod handlerMethod, String path) {
        String simpleName = handlerMethod.getBeanType().getSimpleName();
        if (simpleName.endsWith("Controller")) {
            return simpleName.substring(0, simpleName.length() - "Controller".length());
        }

        String[] parts = path.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "Unknown";
    }

    private String resolveEntityId(HttpServletRequest request) {
        Object variablesObj = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variablesObj instanceof Map<?, ?> variables)) {
            return "N/A";
        }

        if (variables.containsKey("staffUuid")) {
            return String.valueOf(variables.get("staffUuid"));
        }
        if (variables.containsKey("studentId")) {
            return String.valueOf(variables.get("studentId"));
        }
        if (variables.containsKey("guardianId")) {
            return String.valueOf(variables.get("guardianId"));
        }
        if (variables.containsKey("userType")) {
            return String.valueOf(variables.get("userType"));
        }

        return variables.values().stream().findFirst().map(String::valueOf).orElse("N/A");
    }

    private Map<String, Object> buildPayload(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        Map<String, Object> after = new HashMap<>();
        after.put("httpMethod", request.getMethod());
        after.put("requestUri", request.getRequestURI());
        after.put("status", response.getStatus());
        after.put("success", response.getStatus() < 400);
        if (ex != null) {
            after.put("error", ex.getClass().getSimpleName());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("before", Map.of());
        payload.put("after", after);
        return payload;
    }
}



