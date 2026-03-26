package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;

public interface StudentDashboardService {

    IntelligenceResponseDTO getDashboardIntelligence(Long userId, Long academicYearId);
}

