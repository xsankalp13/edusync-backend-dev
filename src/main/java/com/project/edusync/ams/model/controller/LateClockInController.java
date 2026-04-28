package com.project.edusync.ams.model.controller;

import com.project.edusync.ams.model.dto.request.LateClockInReviewDTO;
import com.project.edusync.ams.model.dto.response.LateClockInRequestDTO;
import com.project.edusync.ams.model.enums.LateClockInStatus;
import com.project.edusync.ams.model.service.LateClockInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "${api.url:/api/v1}/auth/ams/late-clockin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Late Clock-In Requests", description = "Admin review of out-of-window attendance clock-ins")
public class LateClockInController {

    private final LateClockInService lateClockInService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_HR_ADMIN')")
    @Operation(summary = "List late clock-in requests", description = "Returns paginated list optionally filtered by status")
    public ResponseEntity<Page<LateClockInRequestDTO>> listRequests(
            @RequestParam(required = false) LateClockInStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(lateClockInService.listRequests(status, pageable));
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_HR_ADMIN')")
    @Operation(summary = "Get count of pending late clock-in requests")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("pending", lateClockInService.countPending()));
    }

    @PutMapping("/{uuid}/review")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_HR_ADMIN')")
    @Operation(summary = "Approve or reject a late clock-in request")
    public ResponseEntity<LateClockInRequestDTO> review(
            @PathVariable UUID uuid,
            @Valid @RequestBody LateClockInReviewDTO dto) {
        Long adminUserId = resolveUserId();
        return ResponseEntity.ok(lateClockInService.review(uuid, dto, adminUserId));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Map<?, ?> details) {
            Object userId = details.get("userId");
            if (userId instanceof Number num) return num.longValue();
            if (userId instanceof String s) return Long.parseLong(s);
        }
        return null;
    }
}
