package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.audit.FinanceAuditLogResponseDTO;
import com.project.edusync.finance.service.implementation.FinanceAuditServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/audit-logs")
@RequiredArgsConstructor
public class FinanceAuditLogController {

    private final FinanceAuditServiceImpl auditService;
    private static final Long SID = 1L;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:reports:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<List<FinanceAuditLogResponseDTO>> getAllLogs() {
        return ResponseEntity.ok(auditService.getAuditLogs(SID));
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @PreAuthorize("hasAnyAuthority('finance:reports:read','ROLE_ADMIN', 'ROLE_SCHOOL_ADMIN','ROLE_FINANCE_ADMIN', 'ROLE_AUDITOR')")
    public ResponseEntity<List<FinanceAuditLogResponseDTO>> getEntityLogs(@PathVariable String entityName, @PathVariable Long entityId) {
        return ResponseEntity.ok(auditService.getAuditLogsForEntity(entityName, entityId));
    }
}
