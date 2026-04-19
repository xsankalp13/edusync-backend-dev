package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.approval.ApprovalChainConfigCreateDTO;
import com.project.edusync.hrms.dto.approval.ApprovalChainConfigResponseDTO;
import com.project.edusync.hrms.model.enums.ApprovalActionType;
import com.project.edusync.hrms.service.ApprovalEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms/approval-chains")
@RequiredArgsConstructor
@Tag(name = "HRMS Approval Chains", description = "Multi-level approval chain configuration")
public class ApprovalChainController {

    private final ApprovalEngineService approvalEngineService;

    @PostMapping
    @Operation(summary = "Create approval chain")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ApprovalChainConfigResponseDTO> create(@Valid @RequestBody ApprovalChainConfigCreateDTO dto) {
        return ResponseEntity.ok(approvalEngineService.createChain(dto));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update approval chain")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ApprovalChainConfigResponseDTO> update(@PathVariable UUID uuid,
                                                                  @Valid @RequestBody ApprovalChainConfigCreateDTO dto) {
        return ResponseEntity.ok(approvalEngineService.updateChain(uuid, dto));
    }

    @GetMapping
    @Operation(summary = "List approval chains")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<ApprovalChainConfigResponseDTO>> list(
            @RequestParam(required = false) ApprovalActionType actionType) {
        return ResponseEntity.ok(approvalEngineService.listChains(actionType));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete (deactivate) approval chain")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        approvalEngineService.deleteChain(uuid);
        return ResponseEntity.noContent().build();
    }
}

