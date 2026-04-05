package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingBulkCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingResponseDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StaffSalaryMappingService {

    Page<StaffSalaryMappingResponseDTO> listMappings(Pageable pageable);

    List<StaffSalaryMappingResponseDTO> getMappingsByStaffId(Long staffId);

    List<StaffSalaryMappingResponseDTO> getMappingsByStaffIdentifier(String staffIdentifier);

    StaffSalaryMappingResponseDTO create(StaffSalaryMappingCreateDTO dto);

    StaffSalaryMappingResponseDTO update(Long mappingId, StaffSalaryMappingUpdateDTO dto);

    StaffSalaryMappingResponseDTO updateByIdentifier(String identifier, StaffSalaryMappingUpdateDTO dto);

    BulkOperationResultDTO bulkCreate(StaffSalaryMappingBulkCreateDTO dto);

    ComputedSalaryBreakdownDTO computeBreakdown(Long mappingId);

    ComputedSalaryBreakdownDTO computeBreakdownByIdentifier(String identifier);
}

