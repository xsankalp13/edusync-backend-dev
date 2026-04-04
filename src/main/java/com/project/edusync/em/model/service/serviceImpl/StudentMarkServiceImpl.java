package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.em.model.dto.RequestDTO.BulkMarkRequestDTO;
import com.project.edusync.em.model.dto.RequestDTO.StudentMarkRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.StudentMarkResponseDTO;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.entity.StudentMark;
import com.project.edusync.em.model.enums.StudentAttendanceStatus;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.StudentMarkRepository;
import com.project.edusync.em.model.service.StudentMarkService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentMarkServiceImpl implements StudentMarkService {

    private final StudentMarkRepository studentMarkRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;

    @Override
    public List<StudentMarkResponseDTO> recordBulkMarks(Long scheduleId, BulkMarkRequestDTO bulkRequest) {
        log.info("Processing bulk marks for scheduleId: {}", scheduleId);

        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));

        // 1. Fetch all existing marks for this schedule to handle "Upsert" efficiently
        Map<Long, StudentMark> existingMarksMap = studentMarkRepository.findByExamSchedule_Id(scheduleId)
                .stream()
                .collect(Collectors.toMap(mark -> mark.getStudent().getId(), Function.identity()));

        List<StudentMark> marksToSave = new ArrayList<>();

        for (StudentMarkRequestDTO dto : bulkRequest.getMarks()) {
            // Validate marks against schedule max marks
            validateMarks(dto, BigDecimal.valueOf(schedule.getMaxMarks()));

            StudentMark mark = existingMarksMap.getOrDefault(dto.getStudentId(), new StudentMark());

            // If it's a new mark, set the relationships
            if (mark.getId() == null) {
                Student student = studentRepository.findById(dto.getStudentId())
                        .orElseThrow(() -> new EdusyncException("UIS-404", "Student not found: " + dto.getStudentId(), HttpStatus.NOT_FOUND));
                mark.setStudent(student);
                mark.setExamSchedule(schedule);
            }

            // Update fields
            mark.setAttendanceStatus(dto.getAttendanceStatus());
            if (dto.getAttendanceStatus() == StudentAttendanceStatus.ABSENT) {
                mark.setMarksObtained(BigDecimal.ZERO); // Or null, depending on policy
                mark.setGrade("ABS");
            } else {
                mark.setMarksObtained(dto.getMarksObtained());
                mark.setGrade(calculateGrade(dto.getMarksObtained(), BigDecimal.valueOf(schedule.getMaxMarks())));
            }
            mark.setRemarks(dto.getRemarks());

            marksToSave.add(mark);
        }

        List<StudentMark> savedMarks = studentMarkRepository.saveAll(marksToSave);
        return savedMarks.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public StudentMarkResponseDTO updateMark(UUID markUuid, StudentMarkRequestDTO requestDTO) {
        StudentMark mark = studentMarkRepository.findByUuid(markUuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Mark entry not found", HttpStatus.NOT_FOUND));

        validateMarks(requestDTO, BigDecimal.valueOf(mark.getExamSchedule().getMaxMarks()));

        mark.setAttendanceStatus(requestDTO.getAttendanceStatus());
        mark.setMarksObtained(requestDTO.getMarksObtained());
        mark.setRemarks(requestDTO.getRemarks());
        // Recalculate grade
        if (requestDTO.getAttendanceStatus() == StudentAttendanceStatus.PRESENT && requestDTO.getMarksObtained() != null) {
            mark.setGrade(calculateGrade(requestDTO.getMarksObtained(), BigDecimal.valueOf(mark.getExamSchedule().getMaxMarks())));
        } else if (requestDTO.getAttendanceStatus() == StudentAttendanceStatus.ABSENT) {
            mark.setGrade("ABS");
        }

        return toResponseDTO(studentMarkRepository.save(mark));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentMarkResponseDTO> getMarksBySchedule(Long scheduleId) {
        if (!examScheduleRepository.existsById(scheduleId)) {
            throw new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND);
        }
        return studentMarkRepository.findByExamSchedule_Id(scheduleId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private void validateMarks(StudentMarkRequestDTO dto, BigDecimal maxMarks) {
        if (dto.getMarksObtained() != null && dto.getMarksObtained().compareTo(maxMarks) > 0) {
            throw new EdusyncException("EM-400",
                    "Marks obtained (" + dto.getMarksObtained() + ") cannot exceed max marks (" + maxMarks + ") for student ID: " + dto.getStudentId(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Placeholder for grade calculation logic.
     * In a real system, this would query the GradeSystem -> GradeScales linked to the class.
     */
    private String calculateGrade(BigDecimal obtained, BigDecimal max) {
        if (obtained == null || max == null || max.compareTo(BigDecimal.ZERO) == 0) return "N/A";

        BigDecimal percentage = obtained.divide(max, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        double p = percentage.doubleValue();

        // Simple hardcoded scale for demonstration. Replace with DB lookup later.
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }

    private StudentMarkResponseDTO toResponseDTO(StudentMark entity) {
        return StudentMarkResponseDTO.builder()
                .markUuid(entity.getUuid())
                .scheduleId(entity.getExamSchedule().getId())
                .studentId(entity.getStudent().getId())
                // Assuming Student has a UserProfile with names. Adjust path if needed based on exact UIS structure.
                .studentName(entity.getStudent().getUserProfile().getFirstName() + " " + entity.getStudent().getUserProfile().getLastName())
                .enrollmentNumber(entity.getStudent().getEnrollmentNumber())
                .marksObtained(entity.getMarksObtained())
                .attendanceStatus(entity.getAttendanceStatus())
                .grade(entity.getGrade())
                .remarks(entity.getRemarks())
                .build();
    }
}