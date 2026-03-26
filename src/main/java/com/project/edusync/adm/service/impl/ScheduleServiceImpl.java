package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.AlreadyBookedException;
import com.project.edusync.adm.exception.InvalidRequestException;
import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.dto.response.TimetableOverviewResponseDto;
import com.project.edusync.adm.model.entity.*;
import com.project.edusync.adm.model.enums.ScheduleStatus;
import com.project.edusync.adm.repository.*;
import com.project.edusync.adm.service.ScheduleService;
//import com.project.edusync.exception.DataConflictException;
import com.project.edusync.uis.model.entity.details.TeacherDetails; // Assumed path
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    // Repositories for all related entities
    private final ScheduleRepository scheduleRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final com.project.edusync.uis.repository.details.TeacherDetailsRepository teacherDetailsRepository; // Assumed
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final CacheManager cacheManager;


    @Override
    @Cacheable(value = "sectionSchedules", key = "#sectionId")
    public List<ScheduleResponseDto> getScheduleForSection(UUID sectionId) {
        log.info("Fetching schedule for section id: {}", sectionId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            log.warn("Section with id {} not found", sectionId);
            throw new ResourceNotFoundException("No section resource found with id: " + sectionId);
        }
        List<Schedule> schedules = scheduleRepository.findAllActiveBySectionUuid(sectionId);
        return schedules.stream()
                .map(this::toScheduleResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableOverviewResponseDto> getScheduleOverview() {
        return scheduleRepository.findTimetableOverview().stream()
                .map(row -> TimetableOverviewResponseDto.builder()
                        .classId(row.getClassId())
                        .className(row.getClassName())
                        .sectionId(row.getSectionId())
                        .sectionName(row.getSectionName())
                        .scheduleStatus(row.getScheduleStatus())
                        .totalPeriods(row.getTotalPeriods())
                        .createdAt(row.getCreatedAt() == null ? null : row.getCreatedAt().atOffset(ZoneOffset.UTC))
                        .lastUpdatedAt(row.getLastUpdatedAt() == null ? null : row.getLastUpdatedAt().atOffset(ZoneOffset.UTC))
                        .build())
                .toList();
    }

    @Override
    @CacheEvict(value = "sectionSchedules", key = "#requestDto.sectionId")
    public ScheduleResponseDto addSchedule(ScheduleRequestDto requestDto) {
        log.info("Attempting to create a new schedule entry for section {} at timeslot {}",
                requestDto.getSectionId(), requestDto.getTimeslotId());

        // 1. Validate for conflicts
        validateScheduleConflicts(requestDto, null);

        // 2. Fetch all related entities
        Schedule newSchedule = new Schedule();
        newSchedule.setSection(findSectionById(requestDto.getSectionId()));
        newSchedule.setSubject(findSubjectById(requestDto.getSubjectId()));
        newSchedule.setTeacher(findTeacherById(requestDto.getTeacherId()));
        newSchedule.setRoom(findRoomById(requestDto.getRoomId()));
        newSchedule.setTimeslot(findTimeslotById(requestDto.getTimeslotId()));
        newSchedule.setIsActive(true);
        newSchedule.setStatus(ScheduleStatus.NONE); // Example status

        // 3. Save and return
        Schedule savedSchedule = scheduleRepository.save(newSchedule);
        log.info("Schedule entry {} created successfully", savedSchedule.getUuid());

        return toScheduleResponseDto(savedSchedule);
    }

    @Override
    @Transactional
    public ScheduleResponseDto updateSchedule(UUID scheduleId, ScheduleRequestDto requestDto) {
        log.info("Attempting to update schedule entry {}", scheduleId);

        // 1. Find the existing schedule
        Schedule existingSchedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(() -> {
                    log.warn("No active schedule with id {} to update", scheduleId);
                    return new ResourceNotFoundException("No resource found to update with id: " + scheduleId);
                });

        UUID previousSectionId = existingSchedule.getSection().getUuid();

        // 2. Validate for conflicts (excluding the current scheduleId)
        validateScheduleConflicts(requestDto, scheduleId);

        // 3. Fetch and update all related entities
        existingSchedule.setSection(findSectionById(requestDto.getSectionId()));
        existingSchedule.setSubject(findSubjectById(requestDto.getSubjectId()));
        existingSchedule.setTeacher(findTeacherById(requestDto.getTeacherId()));
        existingSchedule.setRoom(findRoomById(requestDto.getRoomId()));
        existingSchedule.setTimeslot(findTimeslotById(requestDto.getTimeslotId()));

        // 4. Save and return
        Schedule updatedSchedule = scheduleRepository.save(existingSchedule);
        log.info("Schedule entry {} updated successfully", updatedSchedule.getUuid());

        evictSectionScheduleCache(previousSectionId);
        evictSectionScheduleCache(updatedSchedule.getSection().getUuid());

        return toScheduleResponseDto(updatedSchedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID scheduleId) {
        log.info("Attempting to soft delete schedule entry {}", scheduleId);
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(() -> {
                    log.warn("Failed to delete. Schedule not found with id: {}", scheduleId);
                    return new ResourceNotFoundException("Schedule id: " + scheduleId + " not found.");
                });

        scheduleRepository.softDeleteById(scheduleId);
        evictSectionScheduleCache(schedule.getSection().getUuid());
        log.info("Schedule entry {} marked as inactive successfully", scheduleId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "sectionSchedules", key = "#sectionId")
    public void saveAsDraft(UUID sectionId, String statusType) {
        log.info("Attemting to save all the schedules with section id {} as draft", sectionId);
        if (!"draft".equalsIgnoreCase(statusType) && !"publish".equalsIgnoreCase(statusType)) {
            log.warn("Status type {} is not supported", statusType);
            throw new InvalidRequestException("Status type: " + statusType + " is not supported");
        }
        List<Schedule>  schedules = scheduleRepository.findAllActiveBySectionUuid(sectionId);
        for (Schedule schedule : schedules) {
            if ("draft".equalsIgnoreCase(statusType)) {
                schedule.setStatus(ScheduleStatus.DRAFT);
            }

            else {
                schedule.setStatus(ScheduleStatus.PUBLISHED);
            }
        }
        scheduleRepository.saveAll(schedules);
        log.info("Schedule entry with sectionId {} saved successfully as draft", sectionId);

    }

    @Override
    @Transactional
    @CacheEvict(value = "sectionSchedules", key = "#sectionId")
    public List<ScheduleResponseDto> replaceSectionScheduleBulk(UUID sectionId, List<ScheduleRequestDto> schedules) {
        log.info("Attempting bulk schedule replace for sectionId={} payloadSize={}", sectionId, schedules == null ? 0 : schedules.size());

        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new ResourceNotFoundException("No section resource found with id: " + sectionId);
        }
        if (schedules == null || schedules.isEmpty()) {
            throw new InvalidRequestException("Bulk schedule payload must contain at least one schedule entry.");
        }

        validateBulkPayload(sectionId, schedules);

        scheduleRepository.softDeleteBySectionId(sectionId);

        List<Schedule> newSchedules = schedules.stream()
                .map(this::buildSchedule)
                .toList();

        List<Schedule> saved = scheduleRepository.saveAll(newSchedules);
        log.info("Bulk schedule replace completed for sectionId={} inserted={}", sectionId, saved.size());
        return saved.stream().map(this::toScheduleResponseDto).toList();
    }


    // --- Private Helper Methods ---

    /**
     * Validates for teacher, room, and section double-booking.
     * @param dto The DTO with new schedule details.
     * @param scheduleId The ID of the schedule to exclude from checks (null for creates).
     */
    private void validateScheduleConflicts(ScheduleRequestDto dto, UUID scheduleId) {
        if (scheduleRepository.existsTeacherConflict(dto.getTeacherId(), dto.getTimeslotId(), scheduleId)) {
            log.warn("Validation failed: Teacher {} is already booked at timeslot {}", dto.getTeacherId(), dto.getTimeslotId());
            throw new AlreadyBookedException("Teacher is already booked at this time.");
        }
        if (scheduleRepository.existsRoomConflict(dto.getRoomId(), dto.getTimeslotId(), scheduleId)) {
            log.warn("Validation failed: Room {} is already booked at timeslot {}", dto.getRoomId(), dto.getTimeslotId());
            throw new AlreadyBookedException("Room is already booked at this time.");
        }
        if (scheduleRepository.existsSectionConflict(dto.getSectionId(), dto.getTimeslotId(), scheduleId)) {
            log.warn("Validation failed: Section {} is already scheduled at timeslot {}", dto.getSectionId(), dto.getTimeslotId());
            throw new AlreadyBookedException("Section is already scheduled at this time.");
        }
    }

    private void validateBulkPayload(UUID sectionId, List<ScheduleRequestDto> schedules) {
        Set<String> teacherTimeslot = new HashSet<>();
        Set<String> roomTimeslot = new HashSet<>();
        Set<UUID> sectionTimeslot = new HashSet<>();

        for (ScheduleRequestDto dto : schedules) {
            if (!sectionId.equals(dto.getSectionId())) {
                throw new InvalidRequestException("Each schedule row must use the same sectionId as the path parameter.");
            }

            validateScheduleConflicts(dto, null);

            String teacherKey = dto.getTeacherId() + "#" + dto.getTimeslotId();
            if (!teacherTimeslot.add(teacherKey)) {
                throw new InvalidRequestException("Duplicate teacher-timeslot pair detected in bulk payload.");
            }

            String roomKey = dto.getRoomId() + "#" + dto.getTimeslotId();
            if (!roomTimeslot.add(roomKey)) {
                throw new InvalidRequestException("Duplicate room-timeslot pair detected in bulk payload.");
            }

            if (!sectionTimeslot.add(dto.getTimeslotId())) {
                throw new InvalidRequestException("Duplicate section-timeslot pair detected in bulk payload.");
            }
        }
    }

    private Schedule buildSchedule(ScheduleRequestDto requestDto) {
        Schedule schedule = new Schedule();
        schedule.setSection(findSectionById(requestDto.getSectionId()));
        schedule.setSubject(findSubjectById(requestDto.getSubjectId()));
        schedule.setTeacher(findTeacherById(requestDto.getTeacherId()));
        schedule.setRoom(findRoomById(requestDto.getRoomId()));
        schedule.setTimeslot(findTimeslotById(requestDto.getTimeslotId()));
        schedule.setIsActive(true);
        schedule.setStatus(ScheduleStatus.DRAFT);
        return schedule;
    }

    private void evictSectionScheduleCache(UUID sectionId) {
        if (sectionId == null || cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache("sectionSchedules");
        if (cache != null) {
            cache.evict(sectionId);
        }
    }

    // --- Entity Finder Helpers ---

    private Section findSectionById(UUID sectionId) {
        return sectionRepository.findById(sectionId).orElseThrow(() ->
                new ResourceNotFoundException("Section not found with id: " + sectionId));
    }

    private Subject findSubjectById(UUID subjectId) {
        return subjectRepository.findActiveById(subjectId).orElseThrow(() ->
                new ResourceNotFoundException("Subject not found with id: " + subjectId));
    }

    private TeacherDetails findTeacherById(Long teacherId) {
        // Assuming you have a findActiveById method in TeacherDetailsRepository
        return teacherDetailsRepository.findActiveById(teacherId).orElseThrow(() ->
                new ResourceNotFoundException("Teacher not found with id: " + teacherId));
    }

    private Room findRoomById(UUID roomId) {
        return roomRepository.findActiveById(roomId).orElseThrow(() ->
                new ResourceNotFoundException("Room not found with id: " + roomId));
    }

    private Timeslot findTimeslotById(UUID timeslotId) {
        return timeslotRepository.findActiveById(timeslotId).orElseThrow(() ->
                new ResourceNotFoundException("Timeslot not found with id: " + timeslotId));
    }


    /**
     * Private helper to convert Schedule Entity to the rich Response DTO.
     */
    private ScheduleResponseDto toScheduleResponseDto(Schedule entity) {
        if (entity == null) return null;

        return ScheduleResponseDto.builder()
                .uuid(entity.getUuid())
                .section(ScheduleResponseDto.NestedSectionResponseDto.builder()
                        .uuid(entity.getSection().getUuid())
                        .sectionName(entity.getSection().getSectionName())
                        .className(entity.getSection().getAcademicClass().getName())
                        .build())
                .subject(ScheduleResponseDto.NestedSubjectResponseDto.builder()
                        .uuid(entity.getSubject().getUuid())
                        .name(entity.getSubject().getName())
                        .subjectCode(entity.getSubject().getSubjectCode())
                        .build())
                .teacher(ScheduleResponseDto.NestedTeacherResponseDto.builder()
                        .id(entity.getTeacher().getStaff().getId())
                        .name(entity.getTeacher().getStaff().getUserProfile().getFirstName()) // Assumed path to name
                        .build())
                .room(ScheduleResponseDto.NestedRoomResponseDto.builder()
                        .uuid(entity.getRoom().getUuid())
                        .name(entity.getRoom().getName())
                        .roomType(entity.getRoom().getRoomType())
                        .build())
                .timeslot(ScheduleResponseDto.NestedTimeslotResponseDto.builder()
                        .uuid(entity.getTimeslot().getUuid())
                        .dayOfWeek(entity.getTimeslot().getDayOfWeek())
                        .startTime(entity.getTimeslot().getStartTime())
                        .endTime(entity.getTimeslot().getEndTime())
                        .slotLabel(entity.getTimeslot().getSlotLabel())
                        .build())
                .build();
    }
}