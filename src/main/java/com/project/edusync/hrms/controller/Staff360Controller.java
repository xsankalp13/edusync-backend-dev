package com.project.edusync.hrms.controller;

import com.project.edusync.hrms.dto.staff360.Staff360ProfileDTO;
import com.project.edusync.hrms.service.impl.Staff360ServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.url}/auth/hrms/staff/{staffRef}/360-profile")
@RequiredArgsConstructor
@Tag(name = "HRMS Staff 360 Profile", description = "Aggregated 360° staff profile view")
public class Staff360Controller {

    private final Staff360ServiceImpl staff360Service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_SCHOOL_ADMIN','ROLE_ADMIN')")
    @Operation(summary = "Get 360° profile for a staff member")
    public ResponseEntity<Staff360ProfileDTO> getProfile(@PathVariable String staffRef) {
        return ResponseEntity.ok(staff360Service.getProfile(staffRef));
    }
}

