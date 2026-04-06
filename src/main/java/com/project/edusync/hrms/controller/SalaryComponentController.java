package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.salary.SalaryComponentCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentUpdateDTO;
import com.project.edusync.hrms.service.SalaryComponentService;
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
@RequestMapping("${api.url}/auth/hrms/salary/components")
@RequiredArgsConstructor
@Tag(name = "HRMS Salary", description = "Salary component master APIs")
public class SalaryComponentController {

    private final SalaryComponentService salaryComponentService;

    @GetMapping
    @Operation(summary = "List salary components")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<SalaryComponentResponseDTO>> listAll() {
        return ResponseEntity.ok(salaryComponentService.listAll());
    }

    @PostMapping
    @Operation(summary = "Create salary component")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<SalaryComponentResponseDTO> create(@Valid @RequestBody SalaryComponentCreateDTO dto) {
        return new ResponseEntity<>(salaryComponentService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update salary component")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<SalaryComponentResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody SalaryComponentUpdateDTO dto
    ) {
        return ResponseEntity.ok(salaryComponentService.updateByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Soft delete salary component")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        salaryComponentService.deleteByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }
}

