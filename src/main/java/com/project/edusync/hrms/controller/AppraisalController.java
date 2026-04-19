package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.appraisal.AppraisalDTOs.*;
import com.project.edusync.hrms.model.enums.AppraisalCycleStatus;
import com.project.edusync.hrms.service.impl.AppraisalServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/appraisals")
@RequiredArgsConstructor
@Tag(name = "HRMS Appraisals", description = "Performance appraisal management")
public class AppraisalController {

    private final AppraisalServiceImpl appraisalService;

    @PostMapping("/cycles")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Create appraisal cycle")
    public ResponseEntity<CycleResponseDTO> createCycle(@RequestBody CycleCreateDTO dto) {
        return ResponseEntity.ok(appraisalService.createCycle(dto));
    }

    @GetMapping("/cycles")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List appraisal cycles")
    public ResponseEntity<List<CycleResponseDTO>> listCycles() {
        return ResponseEntity.ok(appraisalService.listCycles());
    }

    @GetMapping("/cycles/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get cycle detail")
    public ResponseEntity<CycleDetailDTO> getCycle(@PathVariable UUID uuid) {
        return ResponseEntity.ok(appraisalService.getCycleDetail(uuid));
    }

    @PatchMapping("/cycles/{uuid}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Update cycle status")
    public ResponseEntity<CycleResponseDTO> updateStatus(@PathVariable UUID uuid, @RequestParam AppraisalCycleStatus status) {
        return ResponseEntity.ok(appraisalService.updateCycleStatus(uuid, status));
    }

    @PostMapping("/cycles/{uuid}/goals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add goal to cycle")
    public ResponseEntity<GoalResponseDTO> addGoal(@PathVariable UUID uuid, @RequestBody GoalCreateDTO dto) {
        return ResponseEntity.ok(appraisalService.addGoal(uuid, dto));
    }

    @GetMapping("/cycles/{uuid}/goals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List goals for cycle")
    public ResponseEntity<List<GoalResponseDTO>> listGoals(@PathVariable UUID uuid,
                                                             @RequestParam(required = false) String staffRef) {
        return ResponseEntity.ok(appraisalService.listGoals(uuid, staffRef));
    }

    @PostMapping("/cycles/{uuid}/self-review")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit self appraisal")
    public ResponseEntity<SelfReviewResponseDTO> selfReview(@PathVariable UUID uuid, @RequestBody SelfReviewCreateDTO dto) {
        return ResponseEntity.ok(appraisalService.submitSelfReview(uuid, dto));
    }

    @PostMapping("/cycles/{uuid}/manager-review")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Submit manager appraisal")
    public ResponseEntity<ManagerReviewResponseDTO> managerReview(@PathVariable UUID uuid, @RequestBody ManagerReviewCreateDTO dto) {
        return ResponseEntity.ok(appraisalService.submitManagerReview(uuid, dto));
    }
}

