package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeDTO;
import com.project.edusync.finance.service.ScholarshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/scholarships")
@RequiredArgsConstructor
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    @PostMapping("/types")
    public ResponseEntity<ScholarshipTypeDTO> createType(@RequestBody @Valid ScholarshipTypeCreateDTO dto) {
        return ResponseEntity.ok(scholarshipService.createType(dto));
    }

    @GetMapping("/types")
    public ResponseEntity<List<ScholarshipTypeDTO>> getAllTypes() {
        return ResponseEntity.ok(scholarshipService.getAllTypes());
    }

    @PostMapping("/assignments")
    public ResponseEntity<ScholarshipAssignmentDTO> assignScholarship(@RequestBody @Valid ScholarshipAssignmentCreateDTO dto) {
        return ResponseEntity.ok(scholarshipService.assignScholarship(dto));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<ScholarshipAssignmentDTO>> getAllAssignments() {
        return ResponseEntity.ok(scholarshipService.getAllAssignments());
    }

    @PutMapping("/assignments/{id}/revoke")
    public ResponseEntity<ScholarshipAssignmentDTO> revokeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(scholarshipService.revokeAssignment(id));
    }

    @PutMapping("/assignments/{id}/activate")
    public ResponseEntity<ScholarshipAssignmentDTO> activateAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(scholarshipService.activateAssignment(id));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        scholarshipService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/assignments/bulk")
    public ResponseEntity<Void> deleteBulkAssignments(@RequestBody List<Long> ids) {
        scholarshipService.deleteBulkAssignments(ids);
        return ResponseEntity.noContent().build();
    }
}
