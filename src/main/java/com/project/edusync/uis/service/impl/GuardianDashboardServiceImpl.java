package com.project.edusync.uis.service.impl;

import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.em.model.repository.StudentMarkRepository;
import com.project.edusync.uis.model.dto.dashboard.IntelligenceResponseDTO;
import com.project.edusync.uis.model.dto.dashboard.OverviewResponseDTO;
import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import com.project.edusync.uis.repository.GuardianRepository;
import com.project.edusync.uis.repository.StudentGuardianRelationshipRepository;
import com.project.edusync.uis.repository.StudentRepository;
import com.project.edusync.uis.service.DashboardAggregatorService;
import com.project.edusync.uis.service.GuardianDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardianDashboardServiceImpl implements GuardianDashboardService {
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final StudentRepository studentRepository;
    private final DashboardAggregatorService dashboardAggregatorService;

    /**
     * Returns dashboard intelligence for all students linked to the guardian.
     */
    @Override
    @Transactional(readOnly = true)
    public List<IntelligenceResponseDTO> getAllLinkedStudentsDashboardIntelligence(Long guardianUserId, Long academicYearId) {
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(guardianUserId)
                .orElseThrow(() -> new IllegalArgumentException("Guardian not found for userId=" + guardianUserId));
        List<StudentGuardianRelationship> relationships = relationshipRepository.findByGuardian(guardian);
        List<IntelligenceResponseDTO> dashboards = new ArrayList<>();
        for (StudentGuardianRelationship rel : relationships) {
            Student student = rel.getStudent();
            Long studentUserId = student.getUserProfile().getUser().getId();
            dashboards.add(dashboardAggregatorService.getDashboardIntelligence(studentUserId, academicYearId));
        }
        return dashboards;
    }

    /**
     * Returns dashboard overview for all students linked to the guardian.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OverviewResponseDTO> getAllLinkedStudentsDashboardOverview(Long guardianUserId, Long academicYearId) {
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(guardianUserId)
                .orElseThrow(() -> new IllegalArgumentException("Guardian not found for userId=" + guardianUserId));
        List<StudentGuardianRelationship> relationships = relationshipRepository.findByGuardian(guardian);
        List<OverviewResponseDTO> dashboards = new ArrayList<>();
        for (StudentGuardianRelationship rel : relationships) {
            Student student = rel.getStudent();
            Long studentUserId = student.getUserProfile().getUser().getId();
            dashboards.add(dashboardAggregatorService.getDashboardOverview(studentUserId, academicYearId));
        }
        return dashboards;
    }
}

