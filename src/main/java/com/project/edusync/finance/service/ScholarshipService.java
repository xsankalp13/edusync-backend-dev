package com.project.edusync.finance.service;

import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipAssignmentDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeCreateDTO;
import com.project.edusync.finance.dto.scholarship.ScholarshipTypeDTO;

import java.util.List;

public interface ScholarshipService {
    ScholarshipTypeDTO createType(ScholarshipTypeCreateDTO dto);
    List<ScholarshipTypeDTO> getAllTypes();
    
    ScholarshipAssignmentDTO assignScholarship(ScholarshipAssignmentCreateDTO dto);
    List<ScholarshipAssignmentDTO> getAllAssignments();
    ScholarshipAssignmentDTO revokeAssignment(Long assignmentId);
    ScholarshipAssignmentDTO activateAssignment(Long assignmentId);
    void deleteAssignment(Long assignmentId);
    void deleteBulkAssignments(List<Long> assignmentIds);
}
