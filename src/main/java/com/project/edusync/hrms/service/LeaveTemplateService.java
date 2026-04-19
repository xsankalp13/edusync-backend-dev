package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.leavetemplate.BulkAssignByDesignationDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateCreateDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateResponseDTO;
import com.project.edusync.hrms.dto.leavetemplate.LeaveTemplateUpdateDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingRequestDTO;
import com.project.edusync.hrms.dto.leavetemplate.StaffLeaveTemplateMappingResponseDTO;
import com.project.edusync.uis.model.enums.StaffCategory;

import java.util.List;

public interface LeaveTemplateService {
    List<LeaveTemplateResponseDTO> list(String academicYear, StaffCategory category);
    LeaveTemplateResponseDTO getByIdentifier(String identifier);
    LeaveTemplateResponseDTO create(LeaveTemplateCreateDTO dto);
    LeaveTemplateResponseDTO update(String identifier, LeaveTemplateUpdateDTO dto);
    void delete(String identifier);

    StaffLeaveTemplateMappingResponseDTO assignToStaff(String templateRef, StaffLeaveTemplateMappingRequestDTO dto);
    BulkOperationResultDTO bulkAssignByDesignation(String templateRef, BulkAssignByDesignationDTO dto);
    List<StaffLeaveTemplateMappingResponseDTO> getStaffMappings(String staffRef);
}
