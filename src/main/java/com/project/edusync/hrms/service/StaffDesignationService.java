package com.project.edusync.hrms.service;

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
}


