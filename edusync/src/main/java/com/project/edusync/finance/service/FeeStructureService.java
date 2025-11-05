package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.feestructure.FeeStructureCreateDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureResponseDTO;
import com.project.edusync.finance.dto.feestructure.FeeStructureUpdateDTO;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Service interface for managing Fee Structures.
 * Defines the contract for business logic operations.
 */
public interface FeeStructureService {

    /**
     * Creates a new FeeStructure and its associated FeeParticulars.
     *
     * @param createDTO The DTO containing data for the structure and its particulars.
     * @return The response DTO of the newly created structure.
     */
    @Transactional
    FeeStructureResponseDTO createFeeStructure(FeeStructureCreateDTO createDTO);

    /**
     * Retrieves all FeeStructures and their associated FeeParticulars.
     *
     * @return A list of FeeStructureResponseDTOs.
     */
    List<FeeStructureResponseDTO> getAllFeeStructures();

    /**
     * Retrieves a single fee structure by its ID,
     * including its associated particulars.
     *
     * @param structureId The ID of the fee structure.
     * @return The response DTO of the found structure.
     */
    FeeStructureResponseDTO getFeeStructureById(Long structureId);

    /**
     * Updates the core details of a fee structure.
     * Note: This does not update the child particulars.
     *
     * @param structureId The ID of the fee structure to update.
     * @param updateDTO   The DTO with new data.
     * @return The response DTO of the updated structure.
     */
    FeeStructureResponseDTO updateFeeStructure(Long structureId, FeeStructureUpdateDTO updateDTO);

    /**
     * Soft-deletes a fee structure by setting it to inactive.
     *
     * @param structureId The ID of the fee structure to delete.
     */
    void deleteFeeStructure(Long structureId);
}
