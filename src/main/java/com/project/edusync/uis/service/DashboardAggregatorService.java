package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;

public interface DashboardAggregatorService {

    IntelligenceResponseDTO getDashboardIntelligence(Long userId, Long academicYearId);
}

