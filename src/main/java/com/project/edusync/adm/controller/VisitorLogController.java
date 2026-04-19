package com.project.edusync.adm.controller;

import com.project.edusync.adm.model.dto.VisitorLogDto;
import com.project.edusync.adm.service.VisitorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/adm/visitors")
@RequiredArgsConstructor
public class VisitorLogController {

    private final VisitorLogService visitorLogService;

    @PostMapping
    @PreAuthorize("hasAuthority('visitor:manage')")
    public ResponseEntity<VisitorLogDto> createVisitorLog(@RequestBody VisitorLogDto dto) {
        return ResponseEntity.ok(visitorLogService.createVisitorLog(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('visitor:read:all') or hasAuthority('visitor:manage')")
    public ResponseEntity<List<VisitorLogDto>> getVisitorLogs(@RequestParam(defaultValue = "daily") String period) {
        return ResponseEntity.ok(visitorLogService.getVisitorLogs(period));
    }
}
