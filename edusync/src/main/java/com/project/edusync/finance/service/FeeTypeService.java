package com.project.edusync.finance.service;


import com.project.edusync.finance.dto.feetype.FeeTypeCreateUpdateDTO;
import com.project.edusync.finance.dto.feetype.FeeTypeResponseDTO;

import java.util.List;

/**
 * Service interface for managing FeeTypes (e.g., "TUITION", "TRANSPORT").
 */
public interface FeeTypeService {

    /**
     * Creates a new FeeType.
     * @param createDTO The DTO containing the data.
     * @return The newly created FeeType's response DTO.
     */
    FeeTypeResponseDTO createFeeType(FeeTypeCreateUpdateDTO createDTO);

    /**
     * Retrieves a FeeType by its ID.
     * @param id The ID of the FeeType.
     * @return The response DTO.
     */
    FeeTypeResponseDTO getFeeTypeById(Long id);

    /**
     * Retrieves all FeeTypes.
     * @return A list of all FeeType response DTOs.
     */
    List<FeeTypeResponseDTO> getAllFeeTypes();

    /**
     * Updates an existing FeeType.
     * @param id The ID of the FeeType to update.
     * @param updateDTO The DTO with new data.
     * @return The updated FeeType's response DTO.
     */
    FeeTypeResponseDTO updateFeeType(Long id, FeeTypeCreateUpdateDTO updateDTO);

    /**
     * Deletes a FeeType by its ID.
     * @param id The ID of the FeeType to delete.
     */
    void deleteFeeType(Long id);
}