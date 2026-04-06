package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.InvigilationRequestDTO;
import com.project.edusync.em.model.dto.response.InvigilationResponseDTO;
import com.project.edusync.em.model.service.InvigilationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/examination/invigilations")
@RequiredArgsConstructor
public class InvigilationController {
    private final InvigilationService invigilationService;

    @PostMapping
    public ResponseEntity<InvigilationResponseDTO> assignInvigilator(@Validated @RequestBody InvigilationRequestDTO dto) {
        return ResponseEntity.ok(invigilationService.assignInvigilator(dto));
    }

    @GetMapping("/exam/{examScheduleId}")
    public ResponseEntity<List<InvigilationResponseDTO>> getByExam(@PathVariable Long examScheduleId) {
        return ResponseEntity.ok(invigilationService.getInvigilatorsByExam(examScheduleId));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<InvigilationResponseDTO>> getByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(invigilationService.getInvigilationsByStaff(staffId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeInvigilator(@PathVariable Long id) {
        invigilationService.removeInvigilator(id);
        return ResponseEntity.noContent().build();
    }
}

