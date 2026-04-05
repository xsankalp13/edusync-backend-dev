package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.salary.SalaryComponentCreateDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentResponseDTO;
import com.project.edusync.hrms.dto.salary.SalaryComponentUpdateDTO;

import java.util.List;

public interface SalaryComponentService {

    List<SalaryComponentResponseDTO> listAll();

    SalaryComponentResponseDTO create(SalaryComponentCreateDTO dto);

    SalaryComponentResponseDTO update(Long componentId, SalaryComponentUpdateDTO dto);

    SalaryComponentResponseDTO updateByIdentifier(String identifier, SalaryComponentUpdateDTO dto);

    void delete(Long componentId);

    void deleteByIdentifier(String identifier);
}

