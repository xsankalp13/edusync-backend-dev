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
import com.project.edusync.uis.repository.UserProfileRepository;
import com.project.edusync.uis.service.DashboardAggregatorService;
import com.project.edusync.uis.service.GuardianDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import com.project.edusync.uis.model.dto.leave.StudentLeaveApplicationRequestDTO;
import com.project.edusync.uis.model.dto.leave.StudentLeaveApplicationResponseDTO;
import com.project.edusync.uis.model.entity.StudentLeaveApplication;
import com.project.edusync.uis.repository.StudentLeaveApplicationRepository;
import com.project.edusync.uis.model.enums.StudentLeaveStatus;
import com.project.edusync.uis.repository.StudentMessageRepository;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardianDashboardServiceImpl implements GuardianDashboardService {
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRelationshipRepository relationshipRepository;
    private final StudentRepository studentRepository;
    private final DashboardAggregatorService dashboardAggregatorService;
    private final StudentLeaveApplicationRepository studentLeaveApplicationRepository;
    private final StudentMessageRepository studentMessageRepository;
    private final UserProfileRepository userProfileRepository;

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

    @Override
    @Transactional
    public StudentLeaveApplicationResponseDTO applyForLeave(Long guardianUserId, Long childId, StudentLeaveApplicationRequestDTO request) {
        // 1. Verify guardian
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(guardianUserId)
                .orElseThrow(() -> new IllegalArgumentException("Guardian not found for userId=" + guardianUserId));

        // 2. Verify student
        Student student = studentRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for id=" + childId));

        // 3. Verify mapping
        boolean linked = relationshipRepository.existsByStudentAndGuardian(student, guardian);
        if (!linked) {
            throw new IllegalArgumentException("Guardian is not linked to student id=" + childId);
        }

        // 4. Validate dates
        LocalDate from = request.getFromDate();
        LocalDate to = request.getToDate();
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid date range");
        }
        if (request.isHalfDay() && from.isBefore(to)) {
            throw new IllegalArgumentException("Half day leave must have fromDate == toDate");
        }

        // 5. Create entity
        StudentLeaveApplication leave = new StudentLeaveApplication();
        leave.setStudent(student);
        leave.setAppliedBy(guardian);
        leave.setFromDate(from);
        leave.setToDate(to);
        leave.setLeaveType(request.getLeaveType());
        leave.setReason(request.getReason());
        leave.setHalfDay(request.isHalfDay());
        leave.setStatus(StudentLeaveStatus.PENDING);

        StudentLeaveApplication saved = studentLeaveApplicationRepository.save(leave);

        Long appliedById = saved.getAppliedBy() != null ? saved.getAppliedBy().getId() : null;

        return new StudentLeaveApplicationResponseDTO(
                saved.getId(),
                saved.getStudent().getId(),
                appliedById,
                saved.getLeaveType(),
                saved.getFromDate(),
                saved.getToDate(),
                saved.getReason(),
                saved.getStatus(),
                saved.isHalfDay()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverviewResponseDTO.AnnouncementDTO> getNotifications(Long userId) {
        // 1. Get messages
        List<OverviewResponseDTO.AnnouncementDTO> notifications = new ArrayList<>();
        
        studentMessageRepository.findUnreadByReceiver(userId).forEach(m -> {
            String senderName = userProfileRepository.findByUser(m.getSender())
                .map(p -> p.getFirstName())
                .orElse("Staff");
                
            notifications.add(new OverviewResponseDTO.AnnouncementDTO(
                m.getId(),
                "New message from " + senderName,
                m.getSentAt().atZone(ZoneId.systemDefault()).toInstant(),
                OverviewResponseDTO.AnnouncementType.ALERT
            ));
        });

        // 2. Get other alerts from linked students
        Guardian guardian = guardianRepository.findByUserProfile_User_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Guardian not found"));
        
        List<StudentGuardianRelationship> relationships = relationshipRepository.findByGuardian(guardian);
        for (StudentGuardianRelationship rel : relationships) {
            Student student = rel.getStudent();
            OverviewResponseDTO overview = dashboardAggregatorService.getDashboardOverview(student.getUserProfile().getUser().getId(), null);
            
            // Add PTM/Events
            if (overview.recentAnnouncements() != null) {
                notifications.addAll(overview.recentAnnouncements());
            }
            
            // Add Homework/Assignments
            if (overview.pendingAssignments() != null) {
                for (var assignment : overview.pendingAssignments()) {
                    notifications.add(new OverviewResponseDTO.AnnouncementDTO(
                        assignment.id(),
                        "Homework: " + assignment.title() + " (" + assignment.subject() + ")",
                        assignment.dueDate(),
                        OverviewResponseDTO.AnnouncementType.ACADEMIC
                    ));
                }
            }
            
            // Add Fees
            if (overview.kpis() != null && overview.kpis().totalOverdueFees() != null && overview.kpis().totalOverdueFees().compareTo(java.math.BigDecimal.ZERO) > 0) {
                notifications.add(new OverviewResponseDTO.AnnouncementDTO(
                    System.currentTimeMillis() + student.getId(),
                    "Fees Overdue for " + student.getUserProfile().getFirstName() + ": " + overview.kpis().totalOverdueFees(),
                    java.time.Instant.now(),
                    OverviewResponseDTO.AnnouncementType.ALERT
                ));
            }
        }

        return notifications.stream()
            .sorted((a, b) -> b.date().compareTo(a.date()))
            .limit(20)
            .collect(Collectors.toList());
    }
}

