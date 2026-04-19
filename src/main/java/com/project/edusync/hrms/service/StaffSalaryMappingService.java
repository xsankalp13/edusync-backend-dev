package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.salary.ComputedSalaryBreakdownDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingBulkCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingCreateDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingResponseDTO;
import com.project.edusync.hrms.dto.salary.StaffSalaryMappingUpdateDTO;
import com.project.edusync.hrms.dto.staff.UnmappedStaffDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StaffSalaryMappingService {

    /** Legacy (all active). */
    Page<StaffSalaryMappingResponseDTO> listMappings(Pageable pageable);

    /** Filtered listing: view = CURRENT | HISTORICAL | ALL, optional gradeCode + templateRef. */
    Page<StaffSalaryMappingResponseDTO> listMappingsFiltered(String view, String gradeCode, String templateRef, Pageable pageable);

    /** Returns active staff who have NO current (today-effective) salary mapping. */
    List<UnmappedStaffDTO> listUnmappedStaff();

    List<StaffSalaryMappingResponseDTO> getMappingsByStaffId(Long staffId);

    List<StaffSalaryMappingResponseDTO> getMappingsByStaffIdentifier(String staffIdentifier);

    StaffSalaryMappingResponseDTO create(StaffSalaryMappingCreateDTO dto);

    StaffSalaryMappingResponseDTO update(Long mappingId, StaffSalaryMappingUpdateDTO dto);

    StaffSalaryMappingResponseDTO updateByIdentifier(String identifier, StaffSalaryMappingUpdateDTO dto);

    void deleteByIdentifier(String identifier);

    BulkOperationResultDTO bulkCreate(StaffSalaryMappingBulkCreateDTO dto);

    ComputedSalaryBreakdownDTO computeBreakdown(Long mappingId);

    ComputedSalaryBreakdownDTO computeBreakdownByIdentifier(String identifier);

    ComputedSalaryBreakdownDTO getMyComputedBreakdown();
}

