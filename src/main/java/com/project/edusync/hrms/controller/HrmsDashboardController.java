package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.dashboard.AttendanceHeatmapDTO;
import com.project.edusync.hrms.dto.dashboard.HrmsDashboardSummaryDTO;
import com.project.edusync.hrms.service.HrmsDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}/auth/hrms/dashboard")
@RequiredArgsConstructor
@Tag(name = "HRMS Dashboard", description = "HRMS dashboard summary APIs")
public class HrmsDashboardController {

    private final HrmsDashboardService hrmsDashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get HRMS dashboard summary")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<HrmsDashboardSummaryDTO> summary() {
        return ResponseEntity.ok(hrmsDashboardService.getSummary());
    }

    @GetMapping("/attendance-heatmap")
    @Operation(summary = "Get attendance heatmap for a given month/year")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<AttendanceHeatmapDTO> attendanceHeatmap(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(hrmsDashboardService.getAttendanceHeatmap(year, month));
    }
}
