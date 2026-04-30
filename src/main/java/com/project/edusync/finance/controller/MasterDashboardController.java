package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.dashboard.DashboardForecastDTO;
import com.project.edusync.finance.dto.dashboard.DashboardKpiTrendsDTO;
import com.project.edusync.finance.dto.dashboard.MasterAnalyticsResponseDTO;
import com.project.edusync.finance.service.MasterDashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.url}/auth/dashboard")
@RequiredArgsConstructor
public class MasterDashboardController {

    private final MasterDashboardAnalyticsService masterDashboardAnalyticsService;

    @GetMapping("/master-analytics")
    public ResponseEntity<MasterAnalyticsResponseDTO> getMasterAnalytics() {
        return ResponseEntity.ok(masterDashboardAnalyticsService.getMasterAnalytics());
    }

    @GetMapping("/kpi-trends")
    public ResponseEntity<DashboardKpiTrendsDTO> getKpiTrends() {
        return ResponseEntity.ok(masterDashboardAnalyticsService.getKpiTrends());
    }

    @GetMapping("/forecast")
    public ResponseEntity<DashboardForecastDTO> getForecast() {
        return ResponseEntity.ok(masterDashboardAnalyticsService.getForecast());
    }
}

