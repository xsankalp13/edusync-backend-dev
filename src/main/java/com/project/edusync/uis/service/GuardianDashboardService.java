package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.model.dto.dashboard.OverviewResponseDTO;

import java.util.List;

public interface GuardianDashboardService {
    List<IntelligenceResponseDTO> getAllLinkedStudentsDashboardIntelligence(Long guardianUserId, Long academicYearId);
    List<OverviewResponseDTO> getAllLinkedStudentsDashboardOverview(Long guardianUserId, Long academicYearId);
}
