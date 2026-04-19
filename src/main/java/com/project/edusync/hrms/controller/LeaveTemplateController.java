package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.leavetemplate.BulkAssignByDesignationDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateCreateDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateResponseDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateUpdateDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingRequestDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingResponseDTO;
import com.project.edusync.hrms.service.LeaveTemplateService;
import com.project.edusync.uis.model.enums.StaffCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/leave-templates")
@RequiredArgsConstructor
public class LeaveTemplateController {

    private final LeaveTemplateService leaveTemplateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<LeaveTemplateResponseDTO>> list(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StaffCategory category
    ) {
        return ResponseEntity.ok(leaveTemplateService.list(academicYear, category));
    }

    @GetMapping("/{ref}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveTemplateResponseDTO> getByIdentifier(@PathVariable String ref) {
        return ResponseEntity.ok(leaveTemplateService.getByIdentifier(ref));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveTemplateResponseDTO> create(@Valid @RequestBody LeaveTemplateCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveTemplateService.create(dto));
    }

    @PutMapping("/{ref}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<LeaveTemplateResponseDTO> update(
            @PathVariable String ref,
            @Valid @RequestBody LeaveTemplateUpdateDTO dto) {
        return ResponseEntity.ok(leaveTemplateService.update(ref, dto));
    }

    @DeleteMapping("/{ref}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String ref) {
        leaveTemplateService.delete(ref);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/{ref}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<StaffLeaveTemplateMappingResponseDTO> assignToStaff(
            @PathVariable String ref,
            @Valid @RequestBody StaffLeaveTemplateMappingRequestDTO dto) {
        return ResponseEntity.ok(leaveTemplateService.assignToStaff(ref, dto));
    }

    @PostMapping("/{ref}/assign-by-designation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<BulkOperationResultDTO> assignByDesignation(
            @PathVariable String ref,
            @Valid @RequestBody BulkAssignByDesignationDTO dto) {
        return ResponseEntity.ok(leaveTemplateService.bulkAssignByDesignation(ref, dto));
    }

    @GetMapping("/staff/{staffRef}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'PRINCIPAL')")
    public ResponseEntity<List<StaffLeaveTemplateMappingResponseDTO>> getStaffMappings(@PathVariable String staffRef) {
        return ResponseEntity.ok(leaveTemplateService.getStaffMappings(staffRef));
    }
}
