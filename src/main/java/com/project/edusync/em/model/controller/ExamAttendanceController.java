package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.ExamAttendanceFinalizeRequestDTO;
import com.project.edusync.em.model.dto.request.ExamAttendanceMarkRequestDTO;
import com.project.edusync.em.model.dto.response.ExamAttendanceFinalizeResponseDTO;
import com.project.edusync.em.model.dto.response.ExamAttendanceMarkResponseDTO;
import com.project.edusync.em.model.dto.response.ExamRoomStudentResponseDTO;
import com.project.edusync.em.model.dto.response.InvigilatorRoomResponseDTO;
import com.project.edusync.em.model.service.ExamAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/examination")
@RequiredArgsConstructor
public class ExamAttendanceController {

    private final ExamAttendanceService examAttendanceService;

    @GetMapping("/invigilator/rooms")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
    public ResponseEntity<List<InvigilatorRoomResponseDTO>> getAssignedRooms() {
        return ResponseEntity.ok(examAttendanceService.getAssignedRoomsForCurrentInvigilator());
    }

    @GetMapping("/exam-attendance/room/{roomId}")
    @PreAuthorize("hasRole('TEACHER') or @examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<List<ExamRoomStudentResponseDTO>> getRoomRoster(@PathVariable Long roomId,
                                                                           @RequestParam Long examScheduleId) {
        return ResponseEntity.ok(examAttendanceService.getRoomAttendanceRoster(roomId, examScheduleId));
    }

    @PostMapping("/exam-attendance/mark")
    @PreAuthorize("hasRole('TEACHER') or @examControllerAccess.canAccessSchedule(#request.examScheduleId)")
    public ResponseEntity<ExamAttendanceMarkResponseDTO> markAttendance(@Valid @RequestBody ExamAttendanceMarkRequestDTO request) {
        return ResponseEntity.ok(examAttendanceService.markAttendance(request));
    }

    @PostMapping("/exam-attendance/finalize")
    @PreAuthorize("hasRole('TEACHER') or @examControllerAccess.canAccessSchedule(#request.examScheduleId)")
    public ResponseEntity<ExamAttendanceFinalizeResponseDTO> finalizeAttendance(@Valid @RequestBody ExamAttendanceFinalizeRequestDTO request) {
        return ResponseEntity.ok(examAttendanceService.finalizeAttendance(request));
    }
}

