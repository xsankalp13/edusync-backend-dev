package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import com.project.edusync.hrms.service.StaffGradeAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/grades")
@RequiredArgsConstructor
@Tag(name = "HRMS Staff Grading", description = "Staff grade assignment and promotion history")
public class StaffGradeAssignmentController {

    private final StaffGradeAssignmentService staffGradeAssignmentService;

    @PostMapping("/assign")
    @Operation(summary = "Assign or promote staff to grade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffGradeAssignmentResponseDTO> assign(@Valid @RequestBody StaffGradeAssignmentCreateDTO dto) {
        return new ResponseEntity<>(staffGradeAssignmentService.assign(dto), HttpStatus.CREATED);
    }

    @GetMapping("/staff/{staffIdentifier}/current")
    @Operation(summary = "Get current staff grade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffGradeAssignmentResponseDTO> current(@PathVariable String staffIdentifier) {
        return ResponseEntity.ok(staffGradeAssignmentService.getCurrentAssignmentByIdentifier(staffIdentifier));
    }

    @GetMapping("/staff/{staffIdentifier}/history")
    @Operation(summary = "Get staff grade history")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<StaffGradeAssignmentResponseDTO>> history(@PathVariable String staffIdentifier) {
        return ResponseEntity.ok(staffGradeAssignmentService.getHistoryByIdentifier(staffIdentifier));
    }

    @GetMapping("/assignments")
    @Operation(summary = "List all current grade assignments")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<StaffGradeAssignmentResponseDTO>> assignments(Pageable pageable) {
        return ResponseEntity.ok(staffGradeAssignmentService.listCurrentAssignments(pageable));
    }
}

