package com.project.edusync.teacher.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.teacher.model.dto.*;
import com.project.edusync.teacher.service.ProxyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Teacher-facing proxy request endpoints.
 *
 * Base path: /api/teacher/proxy-request
 *
 * All endpoints require authentication. Access is derived from the JWT —
 * no teacher-id parameter is needed or accepted (prevents horizontal privilege escalation).
 */
@RestController
@RequestMapping("/api/teacher/proxy-request")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProxyRequestController {

    private final ProxyRequestService proxyRequestService;
    private final AuthUtil authUtil;

    /**
     * GET /api/teacher/proxy-request
     * Returns all proxy requests for the authenticated teacher (sent + received).
     */
    @GetMapping
    public ResponseEntity<List<ProxyRequestResponseDto>> getMyProxyRequests() {
        return ResponseEntity.ok(proxyRequestService.getMyProxyRequests(authUtil.getCurrentUserId()));
    }

    /**
     * GET /api/teacher/proxy-request/today
     * Returns accepted proxy classes the authenticated teacher is covering today.
     */
    @GetMapping("/today")
    public ResponseEntity<List<ProxyRequestResponseDto>> getMyProxyScheduleToday() {
        return ResponseEntity.ok(
                proxyRequestService.getMyProxyScheduleToday(authUtil.getCurrentUserId()));
    }

    /**
     * POST /api/teacher/proxy-request
     * Creates a new peer proxy request from the authenticated teacher to another teacher.
     */
    @PostMapping
    public ResponseEntity<ProxyRequestResponseDto> createPeerRequest(
            @Valid @RequestBody ProxyRequestCreateDto dto) {
        ProxyRequestResponseDto created =
                proxyRequestService.createPeerRequest(authUtil.getCurrentUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/teacher/proxy-request/accept/{requestId}
     * Accepts a proxy request addressed to the authenticated teacher.
     */
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<ProxyRequestResponseDto> acceptProxyRequest(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                proxyRequestService.acceptProxyRequest(requestId, authUtil.getCurrentUserId()));
    }

    /**
     * POST /api/teacher/proxy-request/decline/{requestId}
     * Declines a proxy request addressed to the authenticated teacher.
     */
    @PostMapping("/decline/{requestId}")
    public ResponseEntity<ProxyRequestResponseDto> declineProxyRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) ProxyDeclineDto dto) {
        String reason = (dto != null) ? dto.reason() : null;
        return ResponseEntity.ok(
                proxyRequestService.declineProxyRequest(requestId, authUtil.getCurrentUserId(), reason));
    }

    /**
     * DELETE /api/teacher/proxy-request/{requestId}
     * Cancels a proxy request the authenticated teacher originally sent.
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<ProxyRequestResponseDto> cancelMyRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(
                proxyRequestService.cancelMyRequest(requestId, authUtil.getCurrentUserId()));
    }
}
