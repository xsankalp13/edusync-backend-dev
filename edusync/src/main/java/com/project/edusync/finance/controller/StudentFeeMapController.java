package com.project.edusync.finance.controller;


import com.project.edusync.finance.dto.studentfee.StudentFeeMapCreateDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapResponseDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapUpdateDTO;
import com.project.edusync.finance.service.StudentFeeMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/student-maps") // Base path for student-maps
@RequiredArgsConstructor
public class StudentFeeMapController {

    private final StudentFeeMapService studentFeeMapService;

    /**
     * POST /api/v1/finance/student-maps
     * Creates a new mapping between a student and a fee structure.
     */
    @PostMapping
    public ResponseEntity<StudentFeeMapResponseDTO> createStudentFeeMap(
            @Valid @RequestBody StudentFeeMapCreateDTO createDTO) {

        StudentFeeMapResponseDTO response = studentFeeMapService.createStudentFeeMap(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * PUT /api/v1/finance/student-maps/{mapId}
     * Updates an existing student-fee structure mapping.
     */
    @PutMapping("/{mapId}")
    public ResponseEntity<StudentFeeMapResponseDTO> updateStudentFeeMap(
            @PathVariable Long mapId,
            @Valid @RequestBody StudentFeeMapUpdateDTO updateDTO) {

        StudentFeeMapResponseDTO response = studentFeeMapService.updateStudentFeeMap(mapId, updateDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/student-maps/{mapId}
     * Retrieves a single student-fee structure mapping by its ID.
     */
    @GetMapping("/{mapId}")
    public ResponseEntity<StudentFeeMapResponseDTO> getStudentFeeMapById(@PathVariable Long mapId) {

        StudentFeeMapResponseDTO response = studentFeeMapService.getStudentFeeMapById(mapId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/student-maps
     * Retrieves all student-fee structure mappings.
     */
    @GetMapping
    public ResponseEntity<List<StudentFeeMapResponseDTO>> getAllStudentFeeMaps() {

        List<StudentFeeMapResponseDTO> responseList = studentFeeMapService.getAllStudentFeeMaps();
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }
}
