package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.leave.LeaveTypeConfigCreateDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigResponseDTO;
import com.project.edusync.hrms.dto.leave.LeaveTypeConfigUpdateDTO;
import com.project.edusync.uis.model.enums.StaffCategory;

import java.util.List;

public interface LeaveTypeConfigService {

    List<LeaveTypeConfigResponseDTO> getAll(StaffCategory category);

    LeaveTypeConfigResponseDTO getById(Long leaveTypeId);

    LeaveTypeConfigResponseDTO getByIdentifier(String identifier);

    LeaveTypeConfigResponseDTO create(LeaveTypeConfigCreateDTO dto);

    LeaveTypeConfigResponseDTO update(Long leaveTypeId, LeaveTypeConfigUpdateDTO dto);

    LeaveTypeConfigResponseDTO updateByIdentifier(String identifier, LeaveTypeConfigUpdateDTO dto);

    void delete(Long leaveTypeId);

    void deleteByIdentifier(String identifier);
}

