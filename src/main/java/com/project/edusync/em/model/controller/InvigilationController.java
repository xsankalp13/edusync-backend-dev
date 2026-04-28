package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.InvigilationRequestDTO;
import com.project.edusync.em.model.dto.response.InvigilationResponseDTO;
import com.project.edusync.em.model.service.InvigilationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/examination/invigilations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
public class InvigilationController {
    private final InvigilationService invigilationService;

    @PostMapping
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#dto.examScheduleId)")
    public ResponseEntity<InvigilationResponseDTO> assignInvigilator(@Validated @RequestBody InvigilationRequestDTO dto) {
        return ResponseEntity.ok(invigilationService.assignInvigilator(dto));
    }

    @GetMapping("/exam/{examScheduleId}")
    @PreAuthorize("@examControllerAccess.canAccessSchedule(#examScheduleId)")
    public ResponseEntity<List<InvigilationResponseDTO>> getByExam(@PathVariable Long examScheduleId) {
        return ResponseEntity.ok(invigilationService.getInvigilatorsByExam(examScheduleId));
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('ADMIN','SCHOOL_ADMIN','SUPER_ADMIN','EXAM_CONTROLLER')")
    public ResponseEntity<List<InvigilationResponseDTO>> getByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(invigilationService.getInvigilationsByStaff(staffId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@examControllerAccess.canAccessInvigilation(#id)")
    public ResponseEntity<Void> removeInvigilator(@PathVariable Long id) {
        invigilationService.removeInvigilator(id);
        return ResponseEntity.noContent().build();
    }
}

