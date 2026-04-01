package com.project.edusync.superadmin.audit.controller;

import com.project.edusync.superadmin.audit.model.dto.AuditLogResponseDto;
import com.project.edusync.superadmin.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("${api.url}/super")
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Audit", description = "Paginated and filterable system audit logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search audit logs", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<AuditLogResponseDto>> search(
            @RequestParam(value = "actor", required = false) String actor,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        int clampedSize = Math.min(Math.max(size, 1), 200);
        int pageIndex = Math.max(page, 0);
        Pageable pageable = PageRequest.of(pageIndex, clampedSize, Sort.by("timestamp").descending());

        return ResponseEntity.ok(auditLogService.search(actor, action, entityType, from, to, pageable));
    }
}

