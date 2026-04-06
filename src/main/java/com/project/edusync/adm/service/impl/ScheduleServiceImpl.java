package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.AlreadyBookedException;
import com.project.edusync.adm.exception.InvalidRequestException;
import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.RoomBasicResponseDto;
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
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.*;
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
    public List<ScheduleResponseDto> getScheduleForTeacher(Long staffId) {
        log.info("Fetching schedule for staff id: {}", staffId);
        List<Schedule> schedules = scheduleRepository.findAllActiveByTeacherStaffId(staffId);
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
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sectionSchedules", key = "#requestDto.sectionId"),
            @CacheEvict(value = "editorContext", key = "#requestDto.sectionId"),
            @CacheEvict(value = "availableTeachers", allEntries = true)
    })
    public ScheduleResponseDto addSchedule(ScheduleRequestDto requestDto) {
        log.info("Attempting to create a new schedule entry for section {} at timeslot {}",
                requestDto.getSectionId(), requestDto.getTimeslotId());

        // 1. Inherit section default room when request does not send roomId
        assignRoomIfMissing(requestDto);

        // 2. Ensure the first teaching period of each day is assigned to the class teacher.
        validateClassTeacherForFirstPeriod(requestDto);

        // 3. Validate for conflicts
        validateScheduleConflicts(requestDto, null, null);

        // 4. Build and save the schedule entity
        Schedule newSchedule = new Schedule();
        newSchedule.setSection(findSectionById(requestDto.getSectionId()));
        newSchedule.setSubject(findSubjectById(requestDto.getSubjectId()));
        newSchedule.setTeacher(findTeacherById(requestDto.getTeacherId()));
        newSchedule.setRoom(findRoomById(requestDto.getRoomId()));
        newSchedule.setTimeslot(findTimeslotById(requestDto.getTimeslotId()));
        newSchedule.setIsActive(true);
        newSchedule.setStatus(ScheduleStatus.DRAFT);

        // 5. Save and return
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

        // 1. Inherit section default room when request does not send roomId
        assignRoomIfMissing(requestDto);

        // 2. Ensure the first teaching period of each day is assigned to the class teacher.
        validateClassTeacherForFirstPeriod(requestDto);

        // 3. Validate for conflicts (excluding the current scheduleId)
        validateScheduleConflicts(requestDto, scheduleId, null);

        // 4. Fetch and update all related entities
        existingSchedule.setSection(findSectionById(requestDto.getSectionId()));
        existingSchedule.setSubject(findSubjectById(requestDto.getSubjectId()));
        existingSchedule.setTeacher(findTeacherById(requestDto.getTeacherId()));
        existingSchedule.setRoom(findRoomById(requestDto.getRoomId()));
        existingSchedule.setTimeslot(findTimeslotById(requestDto.getTimeslotId()));

        // 5. Save and return
        Schedule updatedSchedule = scheduleRepository.save(existingSchedule);
        log.info("Schedule entry {} updated successfully", updatedSchedule.getUuid());

        evictSectionScheduleCache(previousSectionId);
        evictSectionScheduleCache(updatedSchedule.getSection().getUuid());
        evictEditorContextCache(previousSectionId);
        evictEditorContextCache(updatedSchedule.getSection().getUuid());

        evictAvailableTeachersCache();

        return toScheduleResponseDto(updatedSchedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID scheduleId) {
        log.info("Attempting to soft delete schedule entry {} (force)", scheduleId);
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(() -> {
                    log.warn("Failed to delete. Schedule not found with id: {}", scheduleId);
                    return new ResourceNotFoundException("Schedule id: " + scheduleId + " not found.");
                });
        // Always perform soft delete, regardless of references
        scheduleRepository.softDeleteById(scheduleId);
        evictSectionScheduleCache(schedule.getSection().getUuid());
        evictEditorContextCache(schedule.getSection().getUuid());
        evictAvailableTeachersCache();
        log.info("Schedule entry {} marked as inactive successfully (forced)", scheduleId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sectionSchedules", key = "#sectionId"),
            @CacheEvict(value = "editorContext", key = "#sectionId")
    })
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
    @Caching(evict = {
            @CacheEvict(value = "sectionSchedules", key = "#sectionId"),
            @CacheEvict(value = "editorContext", key = "#sectionId"),
            @CacheEvict(value = "availableTeachers", allEntries = true)
    })
    public List<ScheduleResponseDto> replaceSectionScheduleBulk(UUID sectionId, List<ScheduleRequestDto> schedules) {
        log.info("Attempting bulk schedule replace for sectionId={} payloadSize={}", sectionId, schedules == null ? 0 : schedules.size());

        Section section = findSectionById(sectionId);
        if (schedules == null || schedules.isEmpty()) {
            throw new InvalidRequestException("Bulk schedule payload must contain at least one schedule entry.");
        }

        // Pre-cache entities for performance
        Map<UUID, Subject> subjectsMap = new HashMap<>();
        Map<Long, TeacherDetails> teachersMap = new HashMap<>();
        Map<UUID, Room> roomsMap = new HashMap<>();
        Map<UUID, Timeslot> timeslotsMap = new HashMap<>();

        // 1. Process each request to assign rooms if missing and validate
        for (ScheduleRequestDto dto : schedules) {
            if (!sectionId.equals(dto.getSectionId())) {
                throw new InvalidRequestException("Each schedule row must use the same sectionId as the path parameter.");
            }

            // If roomId is missing, inherit the section default room
            assignRoomIfMissing(dto);

            // The first teaching period must be handled by the class teacher.
            validateClassTeacherForFirstPeriod(dto);

            // 2. Validate conflicts against OTHER sections
            validateScheduleConflicts(dto, null, sectionId);
        }

        // 2. Internal payload validation (Double-check duplicates in the same payload for remaining logic)
        validateBulkPayloadInternal(schedules);

        // 3. Clear existing schedule
        scheduleRepository.softDeleteBySectionId(sectionId);

        // 4. Build new entities using pre-cached data
        List<Schedule> newSchedules = schedules.stream()
                .map(dto -> {
                    Schedule s = new Schedule();
                    s.setSection(section);
                    s.setSubject(subjectsMap.computeIfAbsent(dto.getSubjectId(), this::findSubjectById));
                    s.setTeacher(teachersMap.computeIfAbsent(dto.getTeacherId(), this::findTeacherById));
                    s.setRoom(roomsMap.computeIfAbsent(dto.getRoomId(), this::findRoomById));
                    s.setTimeslot(timeslotsMap.computeIfAbsent(dto.getTimeslotId(), this::findTimeslotById));
                    s.setIsActive(true);
                    s.setStatus(ScheduleStatus.DRAFT);
                    return s;
                })
                .toList();

        List<Schedule> saved = scheduleRepository.saveAll(newSchedules);
        log.info("Bulk schedule replace completed for sectionId={} inserted={}", sectionId, saved.size());
        return saved.stream().map(this::toScheduleResponseDto).toList();
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "sectionSchedules", key = "#sectionId"),
            @CacheEvict(value = "editorContext", key = "#sectionId"),
            @CacheEvict(value = "availableTeachers", allEntries = true)
    })
    public void deleteScheduleBySection(UUID sectionId) {
        log.info("Attempting complete schedule delete for sectionId={}", sectionId);
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new ResourceNotFoundException("No section resource found with id: " + sectionId);
        }
        scheduleRepository.softDeleteBySectionId(sectionId);
        log.info("Complete schedule delete finished for sectionId={}", sectionId);
    }

    // --- Private Helper Methods ---

    /**
     * Validates for teacher, room, and section double-booking.
     * @param dto The DTO with new schedule details.
     * @param scheduleId The ID of the schedule to exclude from checks (null for creates).
     */
    private void validateScheduleConflicts(ScheduleRequestDto dto, UUID scheduleId, UUID excludeSectionId) {
        Optional<Schedule> teacherConflict = scheduleRepository.findTeacherConflict(dto.getTeacherId(), dto.getTimeslotId(), scheduleId, excludeSectionId);
        if (teacherConflict.isPresent()) {
            Schedule conf = teacherConflict.get();
            String msg = String.format("Teacher '%s' is already booked on %s at %s for Class %s.",
                    conf.getTeacher().getStaff().getUserProfile().getFirstName(),
                    formatDayOfWeek(conf.getTimeslot()),
                    formatTimeslot(conf.getTimeslot()),
                    formatClassSection(conf.getSection()));
            log.warn("Validation failed: {}", msg);
            throw new AlreadyBookedException(msg);
        }

        Optional<Schedule> roomConflict = scheduleRepository.findRoomConflict(dto.getRoomId(), dto.getTimeslotId(), scheduleId, excludeSectionId);
        if (roomConflict.isPresent()) {
            Schedule conf = roomConflict.get();
            String msg = String.format("Room '%s' is already booked on %s at %s for Class %s.",
                    conf.getRoom().getName(),
                    formatDayOfWeek(conf.getTimeslot()),
                    formatTimeslot(conf.getTimeslot()),
                    formatClassSection(conf.getSection()));
            log.warn("Validation failed: {}", msg);
            throw new AlreadyBookedException(msg);
        }

        Optional<Schedule> sectionConflict = scheduleRepository.findSectionConflict(dto.getSectionId(), dto.getTimeslotId(), scheduleId, excludeSectionId);
        if (sectionConflict.isPresent()) {
            Schedule conf = sectionConflict.get();
            String msg = String.format("This section is already scheduled for '%s' on %s at %s.",
                    conf.getSubject().getName(),
                    formatDayOfWeek(conf.getTimeslot()),
                    formatTimeslot(conf.getTimeslot()));
            log.warn("Validation failed: {}", msg);
            throw new AlreadyBookedException(msg);
        }
    }

    private String formatTimeslot(Timeslot timeslot) {
        if (timeslot == null) {
            return "the selected timeslot";
        }
        String label = timeslot.getSlotLabel();
        if (label != null && !label.isBlank()) {
            return label + " (" + timeslot.getStartTime() + "-" + timeslot.getEndTime() + ")";
        }
        return timeslot.getStartTime() + "-" + timeslot.getEndTime();
    }

    private String formatClassSection(Section section) {
        if (section == null || section.getAcademicClass() == null) {
            return "the selected section";
        }
        return section.getAcademicClass().getName() + " " + section.getSectionName();
    }

    private String formatDayOfWeek(Timeslot timeslot) {
        if (timeslot == null || timeslot.getDayOfWeek() == null) {
            return "the selected day";
        }
        return switch (timeslot.getDayOfWeek()) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Day " + timeslot.getDayOfWeek();
        };
    }

    private void assignRoomIfMissing(ScheduleRequestDto dto) {
        if (dto.getRoomId() != null) return;

        Section section = findSectionById(dto.getSectionId());
        Room defaultRoom = section.getDefaultRoom();
        if (defaultRoom == null) {
            throw new InvalidRequestException(
                    "Room ID is required because section '" + section.getSectionName() + "' has no default room configured."
            );
        }
        if (!Boolean.TRUE.equals(defaultRoom.getIsActive())) {
            throw new InvalidRequestException(
                    "Section default room '" + defaultRoom.getName() + "' is inactive. Please select another room."
            );
        }

        dto.setRoomId(defaultRoom.getUuid());
        log.info("Inherited section default room '{}' for section '{}'", defaultRoom.getName(), section.getSectionName());
    }

    private void validateBulkPayloadInternal(List<ScheduleRequestDto> schedules) {
        Set<String> teacherTimeslot = new HashSet<>();
        Set<String> roomTimeslot = new HashSet<>();
        Set<UUID> sectionTimeslot = new HashSet<>();

        for (ScheduleRequestDto dto : schedules) {
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

    private void validateClassTeacherForFirstPeriod(ScheduleRequestDto dto) {
        Section section = findSectionById(dto.getSectionId());
        Timeslot selectedTimeslot = findTimeslotById(dto.getTimeslotId());

        Optional<Timeslot> firstTeachingSlot = timeslotRepository
                .findActiveTeachingByDayOfWeek(selectedTimeslot.getDayOfWeek(), Pageable.ofSize(1))
                .stream()
                .findFirst();
        if (firstTeachingSlot.isEmpty() || !firstTeachingSlot.get().getUuid().equals(selectedTimeslot.getUuid())) {
            return;
        }

        if (section.getClassTeacher() == null || section.getClassTeacher().getId() == null) {
            throw new InvalidRequestException(
                    "Class teacher must be configured for section '" + section.getSectionName() +
                            "' because the first period must be assigned to the class teacher."
            );
        }

        TeacherDetails teacherDetails = findTeacherById(dto.getTeacherId());
        Long assignedStaffId = teacherDetails.getStaff() == null ? null : teacherDetails.getStaff().getId();
        if (!section.getClassTeacher().getId().equals(assignedStaffId)) {
            throw new InvalidRequestException(
                    "For " + formatDayOfWeek(selectedTimeslot) + " first period (" + formatTimeslot(selectedTimeslot) +
                            "), only class teacher can be assigned for section '" + section.getSectionName() + "'."
            );
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

    private void evictEditorContextCache(UUID sectionId) {
        if (sectionId == null || cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache("editorContext");
        if (cache != null) {
            cache.evict(sectionId);
        }
    }

    private void evictAvailableTeachersCache() {
        if (cacheManager == null) {
            return;
        }
        Cache cache = cacheManager.getCache("availableTeachers");
        if (cache != null) {
            cache.clear();
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
                        .defaultRoom(entity.getSection().getDefaultRoom() == null
                                ? null
                                : new RoomBasicResponseDto(
                                        entity.getSection().getDefaultRoom().getUuid(),
                                        entity.getSection().getDefaultRoom().getName()
                                ))
                        .classTeacherUuid(entity.getSection().getClassTeacher() == null ? null : entity.getSection().getClassTeacher().getUuid())
                        .classTeacherName(entity.getSection().getClassTeacher() == null
                                ? null
                                : (entity.getSection().getClassTeacher().getUserProfile().getFirstName() + " " + entity.getSection().getClassTeacher().getUserProfile().getLastName()).trim())
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
                        .totalCapacity(entity.getRoom().getTotalCapacity())
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

