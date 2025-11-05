package com.project.edusync.finance.mapper;


import com.project.edusync.finance.dto.studentfee.StudentFeeMapResponseDTO;
import com.project.edusync.finance.model.entity.StudentFeeMap;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentFeeMapMapper {

    private final ModelMapper modelMapper; // <-- INJECTED

    /**
     * Converts a StudentFeeMap entity to its Response DTO.
     */
    public StudentFeeMapResponseDTO toDto(StudentFeeMap entity) {

        // 1. AUTOMATED: Let ModelMapper handle mapId, effectiveDate, notes
        StudentFeeMapResponseDTO dto = modelMapper.map(entity, StudentFeeMapResponseDTO.class);

        // 2. MANUAL: We handle the complex relationship IDs
        if (entity.getStudent() != null) {
            dto.setStudentId(entity.getStudent().getId());
        }
        if (entity.getFeeStructure() != null) {
            dto.setStructureId(entity.getFeeStructure().getId());
        }

        return dto;
    }
}
