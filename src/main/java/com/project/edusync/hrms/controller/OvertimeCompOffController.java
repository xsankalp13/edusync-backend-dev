package com.project.edusync.hrms.controller;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.overtime.OvertimeDTOs.*;
import com.project.edusync.hrms.model.enums.OvertimeStatus;
import com.project.edusync.hrms.service.impl.OvertimeCompOffServiceImpl;
import com.project.edusync.uis.repository.StaffRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.url}/auth/hrms")
@RequiredArgsConstructor
@Tag(name = "HRMS Overtime & Comp-Off", description = "Overtime records and comp-off management")
public class OvertimeCompOffController {

    private final OvertimeCompOffServiceImpl overtimeService;
    private final AuthUtil authUtil;
    private final StaffRepository staffRepository;

    // Overtime
    @PostMapping("/overtime")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit overtime record")
    public ResponseEntity<OvertimeResponseDTO> create(@RequestBody OvertimeCreateDTO dto) {
        return ResponseEntity.ok(overtimeService.createOvertime(dto));
    }

    @PostMapping("/overtime/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit own overtime record")
    public ResponseEntity<OvertimeResponseDTO> createSelf(@RequestBody OvertimeCreateDTO dto) {
        OvertimeCreateDTO selfDto = new OvertimeCreateDTO(
                resolveCurrentStaffRef(),
                dto.workDate(),
                dto.hoursWorked(),
                dto.reason(),
                dto.compensationType()
        );
        return ResponseEntity.ok(overtimeService.createOvertime(selfDto));
    }

    @GetMapping("/overtime")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List overtime records")
    public ResponseEntity<List<OvertimeResponseDTO>> list(
            @RequestParam(required = false) String staffRef,
            @RequestParam(required = false) OvertimeStatus status) {
        return ResponseEntity.ok(overtimeService.listOvertime(staffRef, status));
    }

    @GetMapping("/overtime/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List own overtime records")
    public ResponseEntity<List<OvertimeResponseDTO>> listSelf(@RequestParam(required = false) OvertimeStatus status) {
        return ResponseEntity.ok(overtimeService.listOvertime(resolveCurrentStaffRef(), status));
    }

    @PostMapping("/overtime/{uuid}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Approve overtime record")
    public ResponseEntity<OvertimeResponseDTO> approve(@PathVariable UUID uuid, @RequestBody(required = false) OvertimeApproveDTO dto) {
        UUID actorRef = resolveActorRef();
        return ResponseEntity.ok(overtimeService.approveOvertime(uuid, actorRef, dto));
    }

    @PostMapping("/overtime/{uuid}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Reject overtime record")
    public ResponseEntity<OvertimeResponseDTO> reject(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(overtimeService.rejectOvertime(uuid, remarks));
    }

    // Comp-Off
    @PostMapping("/comp-off")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Create comp-off record")
    public ResponseEntity<CompOffResponseDTO> createCompOff(@RequestBody CompOffCreateDTO dto) {
        return ResponseEntity.ok(overtimeService.createCompOff(dto));
    }

    @GetMapping("/comp-off/{staffRef}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List comp-off for staff")
    public ResponseEntity<List<CompOffResponseDTO>> listCompOff(@PathVariable String staffRef) {
        return ResponseEntity.ok(overtimeService.listCompOff(staffRef));
    }

    @GetMapping("/comp-off/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List own comp-off records")
    public ResponseEntity<List<CompOffResponseDTO>> listCompOffSelf() {
        return ResponseEntity.ok(overtimeService.listCompOff(resolveCurrentStaffRef()));
    }

    @PostMapping("/comp-off/{uuid}/credit")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Credit comp-off (adds to leave balance)")
    public ResponseEntity<CompOffResponseDTO> credit(@PathVariable UUID uuid) {
        return ResponseEntity.ok(overtimeService.creditCompOff(uuid));
    }

    @GetMapping("/comp-off/summary/{staffRef}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get comp-off balance summary for staff")
    public ResponseEntity<CompOffBalanceSummaryDTO> summary(@PathVariable String staffRef) {
        return ResponseEntity.ok(overtimeService.getCompOffSummary(staffRef));
    }

    @GetMapping("/comp-off/summary/self")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get own comp-off balance summary")
    public ResponseEntity<CompOffBalanceSummaryDTO> selfSummary() {
        return ResponseEntity.ok(overtimeService.getCompOffSummary(resolveCurrentStaffRef()));
    }

    private String resolveCurrentStaffRef() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
                .map(staff -> staff.getUuid().toString())
                .orElseThrow(() -> new EdusyncException(
                        "Authenticated user is not linked to a staff profile",
                        HttpStatus.FORBIDDEN
                ));
    }

    private UUID resolveActorRef() {
        Long userId = authUtil.getCurrentUserId();
        return staffRepository.findByUserProfile_User_Id(userId)
                .map(s -> s.getUuid())
                .orElse(null);
    }
}

