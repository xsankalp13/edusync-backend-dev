package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.approval.ApprovalRequestDetailDTO;
import com.project.edusync.hrms.dto.approval.ApprovalReviewDTO;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.model.enums.ApprovalStatus;
import com.project.edusync.hrms.service.ApprovalEngineService;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.uis.repository.StaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/approval-requests")
@RequiredArgsConstructor
@Tag(name = "HRMS Approval Requests", description = "Approval request lifecycle")
public class ApprovalRequestController {

    private final ApprovalEngineService approvalEngineService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;

    @GetMapping
    @Operation(summary = "List approval requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApprovalRequestDetailDTO>> list(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) ApprovalActionType actionType) {
        return ResponseEntity.ok(approvalEngineService.listRequests(status, actionType));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get approval request detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRequestDetailDTO> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(approvalEngineService.getRequestDetail(uuid));
    }

    @PostMapping("/{uuid}/approve")
    @Operation(summary = "Approve current step")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRequestDetailDTO> approve(@PathVariable UUID uuid,
                                                             @RequestBody(required = false) ApprovalReviewDTO dto,
                                                             Authentication auth) {
        UUID actorRef = resolveActorRef();
        String role = resolveHighestRole(auth);
        approvalEngineService.advance(uuid, actorRef, role, dto != null ? dto.remarks() : null, true);
        return ResponseEntity.ok(approvalEngineService.getRequestDetail(uuid));
    }

    @PostMapping("/{uuid}/reject")
    @Operation(summary = "Reject current step")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApprovalRequestDetailDTO> reject(@PathVariable UUID uuid,
                                                            @RequestBody(required = false) ApprovalReviewDTO dto,
                                                            Authentication auth) {
        UUID actorRef = resolveActorRef();
        String role = resolveHighestRole(auth);
        approvalEngineService.advance(uuid, actorRef, role, dto != null ? dto.remarks() : null, false);
        return ResponseEntity.ok(approvalEngineService.getRequestDetail(uuid));
    }

    private UUID resolveActorRef() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
                .map(s -> s.getUuid())
                .orElse(null);
    }

    private String resolveHighestRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_UNKNOWN");
    }
}

