package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.salary.SalaryTemplateCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryTemplateUpdateDTO;
import com.project.edusync.uis.model.enums.StaffCategory;

import java.util.List;

public interface SalaryTemplateService {

    List<SalaryTemplateResponseDTO> listAll(StaffCategory category);

    SalaryTemplateResponseDTO getById(Long templateId);

    SalaryTemplateResponseDTO getByIdentifier(String identifier);

    SalaryTemplateResponseDTO create(SalaryTemplateCreateDTO dto);

    SalaryTemplateResponseDTO update(Long templateId, SalaryTemplateUpdateDTO dto);

    SalaryTemplateResponseDTO updateByIdentifier(String identifier, SalaryTemplateUpdateDTO dto);

    void delete(Long templateId);

    void deleteByIdentifier(String identifier);
}

