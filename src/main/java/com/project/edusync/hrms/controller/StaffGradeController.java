package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.grade.StaffGradeCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeResponseDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeUpdateDTO;
import com.project.edusync.hrms.service.StaffGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/grades")
@RequiredArgsConstructor
@Tag(name = "HRMS Staff Grading", description = "Staff grade definitions and hierarchy")
public class StaffGradeController {

    private final StaffGradeService staffGradeService;

    @GetMapping
    @Operation(summary = "List grade definitions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<StaffGradeResponseDTO>> listGrades() {
        return ResponseEntity.ok(staffGradeService.listGrades());
    }

    @PostMapping
    @Operation(summary = "Create grade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffGradeResponseDTO> create(@Valid @RequestBody StaffGradeCreateDTO dto) {
        return new ResponseEntity<>(staffGradeService.createGrade(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update grade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<StaffGradeResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody StaffGradeUpdateDTO dto
    ) {
        return ResponseEntity.ok(staffGradeService.updateGradeByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Soft delete grade")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        staffGradeService.deleteGradeByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }
}

