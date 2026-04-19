package com.project.edusync.teacher.controller;

import com.project.edusync.teacher.model.dto.*;
import com.project.edusync.teacher.service.ProxyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-facing proxy management endpoints.
 *
 * Base path: /api/admin/proxy
 *
 * All endpoints require ROLE_SUPER_ADMIN, ROLE_SCHOOL_ADMIN, or ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/proxy")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
public class AdminProxyController {

    private final ProxyRequestService proxyRequestService;

    /**
     * GET /api/admin/proxy?date=YYYY-MM-DD
     * Returns all active (PENDING or ACCEPTED) proxy requests on the given date.
     * Defaults to today if date is omitted.
     */
    @GetMapping
    public ResponseEntity<List<ProxyRequestResponseDto>> getActiveRequestsOnDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(proxyRequestService.getActiveRequestsOnDate(target));
    }

    /**
     * POST /api/admin/proxy/assign
     * Admin directly assigns a proxy teacher for an absent teacher.
     */
    @PostMapping("/assign")
    public ResponseEntity<ProxyRequestResponseDto> assignProxy(
            @Valid @RequestBody AdminAssignProxyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proxyRequestService.adminAssignProxy(dto));
    }

    /**
     * PUT /api/admin/proxy/{requestId}/reassign
     * Reassigns an existing proxy request to a different teacher.
     * Body: { "newProxyUserUuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" }
     */
    @PutMapping("/{requestId}/reassign")
    public ResponseEntity<ProxyRequestResponseDto> reassignProxy(
            @PathVariable Long requestId,
            @RequestBody Map<String, UUID> body) {
        UUID newProxyUserUuid = body.get("newProxyUserUuid");
        if (newProxyUserUuid == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(proxyRequestService.adminReassignProxy(requestId, newProxyUserUuid));
    }

    /**
     * DELETE /api/admin/proxy/{requestId}
     * Admin cancels / removes a proxy request.
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<ProxyRequestResponseDto> cancelProxy(@PathVariable Long requestId) {
        return ResponseEntity.ok(proxyRequestService.adminCancelProxy(requestId));
    }

    /**
     * GET /api/admin/proxy/absent-staff?date=YYYY-MM-DD
     * Returns staff members marked absent on the given date with proxy coverage flag.
     * Defaults to today if date is omitted.
     */
    @GetMapping("/absent-staff")
    public ResponseEntity<List<AbsentStaffDto>> getAbsentStaff(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(proxyRequestService.getAbsentStaffOnDate(target));
    }

    /**
     * GET /api/admin/proxy/load-stats?date=YYYY-MM-DD
     * Returns proxy load statistics (weekly + monthly counts) for every teacher.
     * Defaults to today if date is omitted.
     */
    @GetMapping("/load-stats")
    public ResponseEntity<List<ProxyLoadStatsDto>> getProxyLoadStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(proxyRequestService.getProxyLoadStats(target));
    }
}
