package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.grade.StaffGradeCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeResponseDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeUpdateDTO;

import java.util.List;

public interface StaffGradeService {

    List<StaffGradeResponseDTO> listGrades();

    StaffGradeResponseDTO createGrade(StaffGradeCreateDTO dto);

    StaffGradeResponseDTO updateGrade(Long gradeId, StaffGradeUpdateDTO dto);

    StaffGradeResponseDTO updateGradeByIdentifier(String identifier, StaffGradeUpdateDTO dto);

    void deleteGrade(Long gradeId);

    void deleteGradeByIdentifier(String identifier);
}

