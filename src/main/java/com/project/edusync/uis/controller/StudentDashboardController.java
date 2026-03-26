package com.project.edusync.uis.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/student/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentDashboardService studentDashboardService;
    private final AuthUtil authUtil;

    @GetMapping("/intelligence")
    @PreAuthorize("hasAuthority('profile:read:own')")
    public ResponseEntity<IntelligenceResponseDTO> getDashboardIntelligence() {
        Long userId = authUtil.getCurrentUserId();
        Long academicYearId = authUtil.getCurrentAcademicYearId();
        return ResponseEntity.ok(studentDashboardService.getDashboardIntelligence(userId, academicYearId));
    }
}

