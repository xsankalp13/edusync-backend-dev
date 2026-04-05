package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.salary.SalaryTemplateCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateUpdateDTO;
import com.project.edusync.hrms.service.SalaryTemplateService;
import com.project.edusync.uis.model.enums.StaffCategory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/hrms/salary/templates")
@RequiredArgsConstructor
@Tag(name = "HRMS Salary", description = "Salary template and template-component APIs")
public class SalaryTemplateController {

    private final SalaryTemplateService salaryTemplateService;

    @GetMapping
    @Operation(summary = "List salary templates")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<List<SalaryTemplateResponseDTO>> listAll(@RequestParam(required = false) StaffCategory category) {
        return ResponseEntity.ok(salaryTemplateService.listAll(category));
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get salary template by identifier")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<SalaryTemplateResponseDTO> getByIdentifier(@PathVariable String identifier) {
        return ResponseEntity.ok(salaryTemplateService.getByIdentifier(identifier));
    }

    @PostMapping
    @Operation(summary = "Create salary template")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<SalaryTemplateResponseDTO> create(@Valid @RequestBody SalaryTemplateCreateDTO dto) {
        return new ResponseEntity<>(salaryTemplateService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{identifier}")
    @Operation(summary = "Update salary template")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<SalaryTemplateResponseDTO> update(
            @PathVariable String identifier,
            @Valid @RequestBody SalaryTemplateUpdateDTO dto
    ) {
        return ResponseEntity.ok(salaryTemplateService.updateByIdentifier(identifier, dto));
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Soft delete salary template")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        salaryTemplateService.deleteByIdentifier(identifier);
        return ResponseEntity.noContent().build();
    }
}

