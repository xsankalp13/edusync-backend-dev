package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentCreateDTO;
import com.project.edusync.hrms.dto.grade.StaffGradeAssignmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StaffGradeAssignmentService {

    StaffGradeAssignmentResponseDTO assign(StaffGradeAssignmentCreateDTO dto);

    StaffGradeAssignmentResponseDTO getCurrentAssignment(Long staffId);

    StaffGradeAssignmentResponseDTO getCurrentAssignmentByIdentifier(String staffIdentifier);

    List<StaffGradeAssignmentResponseDTO> getHistory(Long staffId);

    List<StaffGradeAssignmentResponseDTO> getHistoryByIdentifier(String staffIdentifier);

    Page<StaffGradeAssignmentResponseDTO> listCurrentAssignments(Pageable pageable);
}

