package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.ExamControllerAssignmentRequestDTO;
import com.project.edusync.em.model.dto.request.ExamEntryDecisionRequestDTO;
import com.project.edusync.em.model.dto.response.*;
import com.project.edusync.em.model.service.ExamControllerAssignmentService;
import com.project.edusync.em.model.service.ExamControllerDashboardService;
import com.project.edusync.em.model.service.ExamEntryDecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}/auth/examination/exam-controller")
@RequiredArgsConstructor
public class ExamControllerManagementController {

    private final ExamControllerAssignmentService assignmentService;
    private final ExamControllerDashboardService dashboardService;
    private final ExamEntryDecisionService entryDecisionService;

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ExamControllerAssignmentResponseDTO> assignController(
        @Valid @RequestBody ExamControllerAssignmentRequestDTO requestDTO) {
        return ResponseEntity.ok(assignmentService.assignController(requestDTO));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@examControllerAccess.canAccessExam(#examId)")
    public ResponseEntity<ExamControllerDashboardResponseDTO> getDashboard(@RequestParam Long examId) {
        return ResponseEntity.ok(dashboardService.getDashboard(examId));
    }

    @GetMapping("/dashboard/class-view")
    @PreAuthorize("@examControllerAccess.canAccessExam(#examId)")
    public ResponseEntity<ExamControllerClassViewResponseDTO> getClassView(@RequestParam Long examId) {
        return ResponseEntity.ok(dashboardService.getClassView(examId));
    }

    @GetMapping("/dashboard/room-view")
    @PreAuthorize("@examControllerAccess.canAccessExam(#examId)")
    public ResponseEntity<ExamControllerRoomViewResponseDTO> getRoomView(@RequestParam Long examId) {
        return ResponseEntity.ok(dashboardService.getRoomView(examId));
    }

    @PostMapping("/defaulters/decision")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#requestDTO.examScheduleId)")
    public ResponseEntity<ExamEntryDecisionResponseDTO> setEntryDecision(
        @Valid @RequestBody ExamEntryDecisionRequestDTO requestDTO) {
        return ResponseEntity.ok(entryDecisionService.saveDecision(requestDTO));
    }
}

