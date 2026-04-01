package com.project.edusync.superadmin.controller;

import com.project.edusync.superadmin.model.dto.LogTailResponseDto;
import com.project.edusync.superadmin.service.ApplicationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/super")
@RequiredArgsConstructor
@Tag(name = "SuperAdmin Logs", description = "SuperAdmin operations for application log inspection")
public class SuperAdminLogController {

    private final ApplicationLogService applicationLogService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Read recent application logs", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LogTailResponseDto> tailLogs(
            @RequestParam(value = "lines", required = false) Integer lines,
            @RequestParam(value = "level", required = false) String level) {
        return ResponseEntity.ok(applicationLogService.tailLogs(lines, level));
    }
}

