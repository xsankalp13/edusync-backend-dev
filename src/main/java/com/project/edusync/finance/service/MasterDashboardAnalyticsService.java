package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.dashboard.DashboardForecastDTO;
import com.project.edusync.finance.dto.dashboard.DashboardKpiTrendsDTO;
import com.project.edusync.finance.dto.dashboard.MasterAnalyticsResponseDTO;

public interface MasterDashboardAnalyticsService {

    MasterAnalyticsResponseDTO getMasterAnalytics();

    DashboardKpiTrendsDTO getKpiTrends();

    DashboardForecastDTO getForecast();
}

