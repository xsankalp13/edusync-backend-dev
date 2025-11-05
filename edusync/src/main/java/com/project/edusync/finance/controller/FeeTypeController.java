package com.project.edusync.finance.controller;


import com.project.edusync.finance.dto.feetype.FeeTypeCreateUpdateDTO;
import com.project.edusync.finance.dto.feetype.FeeTypeResponseDTO;
import com.project.edusync.finance.service.FeeTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.url}/auth/finance/fee-types") // Base path: /api/v1/finance/fee-types
@RequiredArgsConstructor
public class FeeTypeController {

    private final FeeTypeService feeTypeService;

    /**
     * POST /api/v1/finance/fee-types
     * Creates a new FeeType.
     */
    @PostMapping
    public ResponseEntity<FeeTypeResponseDTO> createFeeType(
            @Valid @RequestBody FeeTypeCreateUpdateDTO createDTO) {

        FeeTypeResponseDTO response = feeTypeService.createFeeType(createDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/finance/fee-types
     * Retrieves all FeeTypes.
     */
    @GetMapping
    public ResponseEntity<List<FeeTypeResponseDTO>> getAllFeeTypes() {
        List<FeeTypeResponseDTO> responseList = feeTypeService.getAllFeeTypes();
        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    /**
     * GET /api/v1/finance/fee-types/{id}
     * Retrieves a single FeeType by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeeTypeResponseDTO> getFeeTypeById(@PathVariable Long id) {
        FeeTypeResponseDTO response = feeTypeService.getFeeTypeById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * PUT /api/v1/finance/fee-types/{id}
     * Updates an existing FeeType.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FeeTypeResponseDTO> updateFeeType(
            @PathVariable Long id,
            @Valid @RequestBody FeeTypeCreateUpdateDTO updateDTO) {

        FeeTypeResponseDTO response = feeTypeService.updateFeeType(id, updateDTO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * DELETE /api/v1/finance/fee-types/{id}
     * Deletes a FeeType.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeeType(@PathVariable Long id) {
        feeTypeService.deleteFeeType(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
