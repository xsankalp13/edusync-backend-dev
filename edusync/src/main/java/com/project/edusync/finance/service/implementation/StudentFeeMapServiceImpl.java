package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.FeeStructureNotFoundException;
import com.project.edusync.common.exception.finance.StudentFeeMapNotFoundException;
import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapCreateDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapResponseDTO;
import com.project.edusync.finance.dto.studentfee.StudentFeeMapUpdateDTO;
import com.project.edusync.finance.mapper.StudentFeeMapMapper;
import com.project.edusync.finance.model.entity.FeeStructure;
import com.project.edusync.finance.model.entity.StudentFeeMap;
import com.project.edusync.finance.repository.FeeStructureRepository;
import com.project.edusync.finance.repository.StudentFeeMapRepository;
import com.project.edusync.finance.service.StudentFeeMapService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentFeeMapServiceImpl implements StudentFeeMapService {

    private final StudentFeeMapRepository studentFeeMapRepository;
    private final StudentRepository studentRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeMapMapper studentFeeMapMapper;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public StudentFeeMapResponseDTO createStudentFeeMap(StudentFeeMapCreateDTO createDTO) {
        // 1. Find the related entities
        Student student = findStudentById(createDTO.getStudentId());
        FeeStructure feeStructure = findFeeStructureById(createDTO.getStructureId());

        // 2. Use ModelMapper for simple fields
        StudentFeeMap studentFeeMap = modelMapper.map(createDTO, StudentFeeMap.class);

        // 3. Manually set the complex relationships
        studentFeeMap.setStudent(student);
        studentFeeMap.setFeeStructure(feeStructure);

        // 4. Save and map to response DTO
        StudentFeeMap savedMap = studentFeeMapRepository.save(studentFeeMap);
        return studentFeeMapMapper.toDto(savedMap);
    }

    @Override
    @Transactional
    public StudentFeeMapResponseDTO updateStudentFeeMap(Long mapId, StudentFeeMapUpdateDTO updateDTO) {
        // 1. Find the existing map
        StudentFeeMap existingMap = studentFeeMapRepository.findById(mapId)
                .orElseThrow(() -> new StudentFeeMapNotFoundException("Student Fee Mapping Not Found for Id: " + mapId));

        // 2. Find the related entities
        Student student = findStudentById(updateDTO.getStudentId());
        FeeStructure feeStructure = findFeeStructureById(updateDTO.getStructureId());

        // 3. Use ModelMapper to update simple fields
        // We configure it to skip nulls, but for a PUT this is standard.
        // For a more robust PATCH, this would be different.
        modelMapper.map(updateDTO, existingMap);

        // 4. Manually update complex relationships
        existingMap.setStudent(student);
        existingMap.setFeeStructure(feeStructure);

        // 5. Save and map to response DTO
        StudentFeeMap updatedMap = studentFeeMapRepository.save(existingMap);
        return studentFeeMapMapper.toDto(updatedMap);
    }

    @Override
    @Transactional
    public StudentFeeMapResponseDTO getStudentFeeMapById(Long mapId) {
        StudentFeeMap studentFeeMap = studentFeeMapRepository.findById(mapId)
                .orElseThrow(() -> new StudentFeeMapNotFoundException("Student Fee Mapping Not Found for Id: " + mapId));
        return studentFeeMapMapper.toDto(studentFeeMap);
    }

    @Override
    @Transactional
    public List<StudentFeeMapResponseDTO> getAllStudentFeeMaps() {
        return studentFeeMapRepository.findAll().stream()
                .map(studentFeeMapMapper::toDto)
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    private Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("" + studentId));
    }

    private FeeStructure findFeeStructureById(Long structureId) {
        return feeStructureRepository.findById(structureId)
                .orElseThrow(() -> new FeeStructureNotFoundException("" + structureId));
    }

}
