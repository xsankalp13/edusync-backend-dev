package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.designation.BulkDesignationAssignRequestDTO;
import com.project.edusync.hrms.dto.designation.BulkDesignationAssignResultDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationCreateUpdateDTO;
import com.project.edusync.hrms.dto.designation.StaffDesignationResponseDTO;
import com.project.edusync.uis.model.enums.StaffCategory;

import java.util.List;

public interface StaffDesignationService {

    List<StaffDesignationResponseDTO> list(StaffCategory category, Boolean active);

    StaffDesignationResponseDTO getById(Long designationId);

    StaffDesignationResponseDTO getByIdentifier(String identifier);

    StaffDesignationResponseDTO create(StaffDesignationCreateUpdateDTO dto);

    StaffDesignationResponseDTO update(Long designationId, StaffDesignationCreateUpdateDTO dto);

    StaffDesignationResponseDTO updateByIdentifier(String identifier, StaffDesignationCreateUpdateDTO dto);

    void delete(Long designationId);

    void deleteByIdentifier(String identifier);

    /**
     * Bulk-assigns a list of staff members (by UUID or employeeId) to the given designation.
     * Runs resiliently: each staff member is processed independently so one failure does not
     * abort the rest. Returns a summary of successes and failures.
     *
     * @param designationRef UUID or id of the target designation
     * @param dto            payload containing the list of staffRefs (UUID or employeeId)
     */
    BulkDesignationAssignResultDTO bulkAssignToDesignation(String designationRef, BulkDesignationAssignRequestDTO dto);

    /**
     * Bulk-unassigns a list of staff members from their current designation.
     * Clears designation to null and reverts jobTitle to the staff's StaffType name.
     * Salary mappings are preserved (staff keeps salary even after losing designation).
     */
    BulkDesignationAssignResultDTO bulkUnassignFromDesignation(BulkDesignationAssignRequestDTO dto);
}


