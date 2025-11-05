package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.studentfee.StudentFeeMapCreateDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapResponseDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapUpdateDTO;

import java.util.List;

public interface StudentFeeMapService {

    /**
     * Maps a Student to a FeeStructure.
     *
     * @param createDTO DTO containing studentId, structureId, and effectiveDate.
     * @return The response DTO of the newly created mapping.
     */
    public StudentFeeMapResponseDTO createStudentFeeMap(StudentFeeMapCreateDTO createDTO);


    /**
     * Updates an existing Student-FeeStructure mapping.
     *
     * @param mapId     The ID of the map to update.
     * @param updateDTO DTO containing the new details.
     * @return The response DTO of the updated mapping.
     */
    StudentFeeMapResponseDTO updateStudentFeeMap(Long mapId, StudentFeeMapUpdateDTO updateDTO);


    /**
     * Retrieves a specific StudentFeeMap by its ID.
     *
     * @param mapId The ID of the map.
     * @return The corresponding response DTO.
     */
    StudentFeeMapResponseDTO getStudentFeeMapById(Long mapId);

    /**
     * Retrieves all Student-FeeStructure mappings.
     *
     * @return A list of all mapping response DTOs.
     */
    List<StudentFeeMapResponseDTO> getAllStudentFeeMaps();

}
