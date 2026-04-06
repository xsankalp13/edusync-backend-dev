package com.project.edusync.em.model.controller;

import com.project.edusync.em.model.dto.request.SittingPlanRequestDTO;
import com.project.edusync.em.model.dto.request.AutoAllocationRequestDTO;
import com.project.edusync.em.model.dto.response.SittingPlanResponseDTO;
import com.project.edusync.em.model.service.SittingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/examination/sitting-plans")
@RequiredArgsConstructor
public class SittingPlanController {
    private final SittingPlanService sittingPlanService;

    @PostMapping
    public ResponseEntity<SittingPlanResponseDTO> assignSeat(@Validated @RequestBody SittingPlanRequestDTO dto) {
        return ResponseEntity.ok(sittingPlanService.assignSeat(dto));
    }

    @PostMapping("/auto-allocate")
    public ResponseEntity<List<SittingPlanResponseDTO>> autoAllocate(@Validated @RequestBody AutoAllocationRequestDTO dto) {
        return ResponseEntity.ok(sittingPlanService.bulkAutoAllocate(dto));
    }

    @GetMapping("/exam/{examScheduleId}")
    public ResponseEntity<List<SittingPlanResponseDTO>> getByExam(@PathVariable Long examScheduleId) {
        return ResponseEntity.ok(sittingPlanService.getSittingPlanByExam(examScheduleId));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<SittingPlanResponseDTO>> getByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(sittingPlanService.getSittingPlanByRoom(roomId));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkRemoveSeats(@RequestBody List<Long> ids) {
        sittingPlanService.bulkRemoveSeatAssignments(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeSeat(@PathVariable Long id) {
        sittingPlanService.removeSeatAssignment(id);
        return ResponseEntity.noContent().build();
    }
}

