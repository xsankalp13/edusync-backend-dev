package com.project.edusync.finance.controller;

import com.project.edusync.finance.dto.feestructure.FeeStructureCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureResponseDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureUpdateDTO;
import com.project.edusync.finance.service.FeeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance") // Base path for the finance module
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    /**
     * POST /api/v1/finance/structures
     * Creates a new fee structure along with its particulars.
     * This matches the Admin API endpoint "POST /structures".
     *
     * @param createDTO The DTO containing structure and particular details.
     * @return A response DTO of the newly created structure.
     */
    @PostMapping("/structures")
    public ResponseEntity<FeeStructureResponseDTO> createFeeStructure( @Valid @RequestBody FeeStructureCreateDTO createDTO ) {
        FeeStructureResponseDTO response = feeStructureService.createFeeStructure(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/finance/structures
     * Retrieves all existing fee structures with their particulars.
     * This matches the Admin API endpoint "GET /structures".
     *
     * @return A list of all fee structure response DTOs.
     */
    @GetMapping("/structures")
    public ResponseEntity<List<FeeStructureResponseDTO>> getAllFeeStructures() {

        List<FeeStructureResponseDTO> responseList = feeStructureService.getAllFeeStructures();
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }


    /**
     * GET /api/v1/finance/structures/{structureId}
     */
    @GetMapping("/{structureId}")
    public ResponseEntity<FeeStructureResponseDTO> getFeeStructureById(
            @PathVariable Long structureId) {

        FeeStructureResponseDTO response = feeStructureService.getFeeStructureById(structureId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * PUT /api/v1/finance/structures/{structureId}
     * Updates the core details of a fee structure.
     */
    @PutMapping("/{structureId}")
    public ResponseEntity<FeeStructureResponseDTO> updateFeeStructure(
            @PathVariable Long structureId,
            @Valid @RequestBody FeeStructureUpdateDTO updateDTO) {

        FeeStructureResponseDTO response = feeStructureService.updateFeeStructure(structureId, updateDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * DELETE /api/v1/finance/structures/{structureId}
     * Soft-deletes a fee structure.
     */
    @DeleteMapping("/{structureId}")
    public ResponseEntity<Void> deleteFeeStructure(@PathVariable Long structureId) {
        feeStructureService.deleteFeeStructure(structureId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
