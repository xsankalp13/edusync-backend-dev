package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.request.AcademicClassRequestDto;
import com.project.edusync.adm.model.dto.response.AcademicClassResponseDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface AcademicClassService {

    @Transactional
    AcademicClassResponseDto addClass(AcademicClassRequestDto academicClassRequestDto);

    @Transactional
    List<AcademicClassResponseDto> getAllClasses();
}
