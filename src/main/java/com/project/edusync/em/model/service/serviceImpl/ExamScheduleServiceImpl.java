package com.project.edusync.em.model.service.serviceImpl;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.repository.SubjectRepository;
import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.common.exception.emException.ExamNotFoundException;
import com.project.edusync.em.model.dto.RequestDTO.ExamScheduleRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.ExamScheduleResponseDTO;
import com.project.edusync.em.model.entity.Exam;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.repository.ExamRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.service.ExamScheduleService;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    // Repositories for ADM entities
    private final AcademicClassRepository academicClassRepository;
    private final SubjectRepository subjectRepository;
    private final com.project.edusync.adm.repository.TimeslotRepository timeslotRepository;
    private final StudentRepository studentRepository;

    private final com.project.edusync.em.model.repository.SittingPlanRepository sittingPlanRepository;
    private final com.project.edusync.em.model.repository.SeatAllocationRepository seatAllocationRepository;
    private final com.project.edusync.em.model.repository.InvigilationRepository invigilationRepository;

    @Override
    public ExamScheduleResponseDTO createSchedule(UUID examUuid, ExamScheduleRequestDTO requestDTO) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new ExamNotFoundException(examUuid));

        validateRequest(requestDTO);

        ExamSchedule schedule = new ExamSchedule();
        schedule.setExam(exam);
        mapDtoToEntity(requestDTO, schedule);

        return mapEntityToResponse(examScheduleRepository.save(schedule));
    }

    @Override
    public ExamScheduleResponseDTO updateSchedule(Long scheduleId, ExamScheduleRequestDTO requestDTO) {
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));

        validateRequest(requestDTO);
        mapDtoToEntity(requestDTO, schedule);

        return mapEntityToResponse(examScheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleResponseDTO> getSchedulesByExam(UUID examUuid) {
        Exam exam = examRepository.findByUuid(examUuid)
                .orElseThrow(() -> new ExamNotFoundException(examUuid));

        return exam.getSchedules().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamScheduleResponseDTO getScheduleById(Long scheduleId) {
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND));
        return mapEntityToResponse(schedule);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        if (!examScheduleRepository.existsById(scheduleId)) {
            throw new EdusyncException("EM-404", "Exam Schedule not found", HttpStatus.NOT_FOUND);
        }
        
        // Cascade delete explicit relationships
        sittingPlanRepository.deleteAllInBatch(sittingPlanRepository.findByExamScheduleId(scheduleId));
        invigilationRepository.deleteAllInBatch(invigilationRepository.findByExamScheduleId(scheduleId));
        seatAllocationRepository.deleteAllInBatch(seatAllocationRepository.findByExamScheduleId(scheduleId));

        examScheduleRepository.deleteById(scheduleId);
    }

    // --- Helper Methods ---

    private void validateRequest(ExamScheduleRequestDTO dto) {
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new EdusyncException("EM-400", "End time must be after start time", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPassingMarks() != null && dto.getPassingMarks().compareTo(dto.getMaxMarks()) > 0) {
            throw new EdusyncException("EM-400", "Passing marks cannot be greater than max marks", HttpStatus.BAD_REQUEST);
        }

        int maxStudentsPerSeat = dto.getMaxStudentsPerSeat() != null ? dto.getMaxStudentsPerSeat() : 1;
        if (maxStudentsPerSeat != 1 && maxStudentsPerSeat != 2) {
            throw new EdusyncException("EM-400", "maxStudentsPerSeat must be 1 or 2", HttpStatus.BAD_REQUEST);
        }

        if (maxStudentsPerSeat == 2 && dto.getSeatSide() == null) {
            throw new EdusyncException("EM-400", "seatSide is required for DOUBLE seating", HttpStatus.BAD_REQUEST);
        }

        if (maxStudentsPerSeat == 1 && dto.getSeatSide() != null) {
            throw new EdusyncException("EM-400", "seatSide must be null for SINGLE seating", HttpStatus.BAD_REQUEST);
        }
    }

    private void mapDtoToEntity(ExamScheduleRequestDTO dto, ExamSchedule entity) {
        AcademicClass academicClass = academicClassRepository.findById(dto.getClassId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Class not found", HttpStatus.NOT_FOUND));
        entity.setAcademicClass(academicClass);

        Subject subject = subjectRepository.findActiveById(dto.getSubjectId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Subject not found", HttpStatus.NOT_FOUND));
        entity.setSubject(subject);

        // --- Timeslot mapping ---
        // NOTE: Timeslot requires dayOfWeek, startTime, endTime. We use examDate.getDayOfWeek().getValue() for dayOfWeek.
        java.time.LocalTime startTime = dto.getStartTime();
        java.time.LocalTime endTime = dto.getEndTime();
        Short dayOfWeek = dto.getExamDate() != null ? (short) dto.getExamDate().getDayOfWeek().getValue() : null;
        com.project.edusync.adm.model.entity.Timeslot timeslot = timeslotRepository
                .findByStartTimeAndEndTime(startTime, endTime)
                .orElseGet(() -> {
                    com.project.edusync.adm.model.entity.Timeslot ts = new com.project.edusync.adm.model.entity.Timeslot();
                    ts.setStartTime(startTime);
                    ts.setEndTime(endTime);
                    ts.setDayOfWeek(dayOfWeek);
                    ts.setIsActive(true);
                    return timeslotRepository.save(ts);
                });
        entity.setTimeslot(timeslot);

        entity.setExamDate(dto.getExamDate());
        entity.setDuration(dto.getDuration());
        entity.setMaxMarks(dto.getMaxMarks().intValue()); // Fix: convert BigDecimal to int

        int maxStudentsPerSeat = dto.getMaxStudentsPerSeat() != null ? dto.getMaxStudentsPerSeat() : 1;
        entity.setMaxStudentsPerSeat(maxStudentsPerSeat);
        entity.setSeatSide(maxStudentsPerSeat == 2 ? dto.getSeatSide() : null);
    }

    private ExamScheduleResponseDTO mapEntityToResponse(ExamSchedule entity) {
        long totalStudents = entity.getSection() != null
                ? studentRepository.countBySectionId(entity.getSection().getId())
                : studentRepository.countBySection_AcademicClass_Id(entity.getAcademicClass().getId());

        return ExamScheduleResponseDTO.builder()
                .scheduleId(entity.getId())
                .examUuid(entity.getExam().getUuid())
                .classId(entity.getAcademicClass().getUuid())
                .className(entity.getAcademicClass().getName())
                .sectionId(entity.getSection() != null ? entity.getSection().getUuid() : null)
                .sectionName(entity.getSection() != null ? entity.getSection().getSectionName() : null)
                .subjectId(entity.getSubject().getUuid())
                .subjectName(entity.getSubject().getName())
                .examDate(entity.getExamDate())
                .startTime(entity.getTimeslot() != null ? entity.getTimeslot().getStartTime() : null)
                .endTime(entity.getTimeslot() != null ? entity.getTimeslot().getEndTime() : null)
                .maxMarks(java.math.BigDecimal.valueOf(entity.getMaxMarks()))
                .passingMarks(java.math.BigDecimal.valueOf(entity.getMaxMarks())) // TODO: replace with actual field if available
                .totalStudents(totalStudents)
                .maxStudentsPerSeat(entity.getMaxStudentsPerSeat())
                .seatSide(entity.getSeatSide())
                .build();
    }
}