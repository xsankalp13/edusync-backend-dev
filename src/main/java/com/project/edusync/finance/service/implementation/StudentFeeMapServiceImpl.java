package com.project.edusync.finance.service.implementation; // Or your '...service' package

import com.project.edusync.common.exception.finance.DuplicateFeeAssignmentException;
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
import com.project.edusync.finance.service.StudentFeeMapService; // Import interface
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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

        Student student = findStudentById(createDTO.getStudentId());
        FeeStructure feeStructure = findFeeStructureById(createDTO.getStructureId());

        // 1.5 Check for duplicate assignment
        if (studentFeeMapRepository.existsByStudent_IdAndFeeStructure_Id(student.getId(), feeStructure.getId())) {
            throw new DuplicateFeeAssignmentException("Student already has this fee structure assigned.");
        }

        // --- FIX: Reverted to manual mapping for entity creation ---
        // 2. Create new entity manually
        StudentFeeMap studentFeeMap = new StudentFeeMap();
        studentFeeMap.setEffectiveDate(createDTO.getEffectiveDate());
        studentFeeMap.setNotes(createDTO.getNotes());

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
                .orElseThrow(() -> new StudentFeeMapNotFoundException("StudentFeeMap not found for ID: " + mapId));

        // 2. Find the related entities
        Student student = findStudentById(updateDTO.getStudentId());
        FeeStructure feeStructure = findFeeStructureById(updateDTO.getStructureId());

        // 3. Use ModelMapper for updates (this is safer)
        modelMapper.map(updateDTO, existingMap);

        // 4. Manually update complex relationships
        existingMap.setStudent(student);
        existingMap.setFeeStructure(feeStructure);

        // 5. Save and map to response DTO
        StudentFeeMap updatedMap = studentFeeMapRepository.save(existingMap);
        return studentFeeMapMapper.toDto(updatedMap);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeeMapResponseDTO getStudentFeeMapById(Long mapId) {
        StudentFeeMap studentFeeMap = studentFeeMapRepository.findById(mapId)
                .orElseThrow(() -> new StudentFeeMapNotFoundException("Student fee mapping doesn't found for ID: " + mapId));
        return studentFeeMapMapper.toDto(studentFeeMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeMapResponseDTO> getAllStudentFeeMaps() {
        return studentFeeMapRepository.findAll().stream()
                .map(studentFeeMapMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<StudentFeeMapResponseDTO> createBulkStudentFeeMaps(List<StudentFeeMapCreateDTO> createDTOs) {
        // 1. Filter out DTOs that already exist in the database to prevent duplicates
        List<StudentFeeMapCreateDTO> filteredDTOs = createDTOs.stream()
                .filter(dto -> !studentFeeMapRepository.existsByStudent_IdAndFeeStructure_Id(dto.getStudentId(), dto.getStructureId()))
                .collect(Collectors.toList());

        if (filteredDTOs.isEmpty()) {
            log.info("Bulk assignment: All students already have this fee structure assigned. Skipping.");
            return List.of();
        }

        List<Long> studentIds = filteredDTOs.stream().map(StudentFeeMapCreateDTO::getStudentId).distinct().collect(Collectors.toList());
        List<Long> structureIds = filteredDTOs.stream().map(StudentFeeMapCreateDTO::getStructureId).distinct().collect(Collectors.toList());

        Map<Long, Student> studentMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, FeeStructure> structureMap = feeStructureRepository.findAllById(structureIds).stream()
                .collect(Collectors.toMap(FeeStructure::getId, fs -> fs));

        List<StudentFeeMap> entities = filteredDTOs.stream().map(dto -> {
            Student student = studentMap.get(dto.getStudentId());
            FeeStructure structure = structureMap.get(dto.getStructureId());
            if (student == null) throw new StudentNotFoundException("Student not found with id: " + dto.getStudentId());
            if (structure == null) throw new FeeStructureNotFoundException("Fee Structure not found with id: " + dto.getStructureId());

            StudentFeeMap entity = new StudentFeeMap();
            entity.setStudent(student);
            entity.setFeeStructure(structure);
            entity.setEffectiveDate(dto.getEffectiveDate());
            entity.setNotes(dto.getNotes());
            return entity;
        }).collect(Collectors.toList());

        return studentFeeMapRepository.saveAll(entities).stream()
                .map(studentFeeMapMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteStudentFeeMap(Long mapId) {
        if (!studentFeeMapRepository.existsById(mapId)) {
            throw new StudentFeeMapNotFoundException("Student fee mapping not found for ID: " + mapId);
        }
        studentFeeMapRepository.deleteById(mapId);
    }

    @Override
    @Transactional
    public void deleteBulkStudentFeeMaps(List<Long> mapIds) {
        log.info("Bulk delete: deleting {} fee mappings", mapIds.size());
        studentFeeMapRepository.deleteAllById(mapIds);
    }

    // --- Private Helper Methods ---

    private Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));
    }

    private FeeStructure findFeeStructureById(Long structureId) {
        return feeStructureRepository.findById(structureId)
                .orElseThrow(() -> new FeeStructureNotFoundException("Fee Structure not found with id:"  + structureId));
    }
}