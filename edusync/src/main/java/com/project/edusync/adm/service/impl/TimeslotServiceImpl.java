package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.model.dto.request.TimeslotRequestDto;
import com.project.edusync.adm.model.dto.response.TimeslotResponseDto;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.repository.TimeslotRepository;
import com.project.edusync.adm.service.TimeslotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeslotServiceImpl implements TimeslotService {

    private final TimeslotRepository timeslotRepository;

    @Override
    @Transactional
    public TimeslotResponseDto addTimeslot(TimeslotRequestDto timeslotRequestDto) {
        log.info("Attempting to create a new timeslot for day {} at {}",
                timeslotRequestDto.getDayOfWeek(), timeslotRequestDto.getStartTime());

        // Best Practice: Validate time logic and uniqueness
        validateTimeslotLogic(timeslotRequestDto, null);

        Timeslot newTimeslot = new Timeslot();
        newTimeslot.setDayOfWeek(timeslotRequestDto.getDayOfWeek());
        newTimeslot.setStartTime(timeslotRequestDto.getStartTime());
        newTimeslot.setEndTime(timeslotRequestDto.getEndTime());
        newTimeslot.setSlotLabel(timeslotRequestDto.getSlotLabel());
        newTimeslot.setIsBreak(timeslotRequestDto.getIsBreak());
        newTimeslot.setIsActive(true);

        Timeslot savedTimeslot = timeslotRepository.save(newTimeslot);
        log.info("Timeslot '{}' created successfully with id {}",
                savedTimeslot.getSlotLabel(), savedTimeslot.getUuid());

        return toTimeslotResponseDto(savedTimeslot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeslotResponseDto> getAllTimeslots(Short dayOfWeek) {
        List<Timeslot> timeslots;
        if (dayOfWeek != null) {
            log.info("Fetching all active timeslots for day: {}", dayOfWeek);
            timeslots = timeslotRepository.findAllActiveByDayOfWeek(dayOfWeek);
        } else {
            log.info("Fetching all active timeslots");
            timeslots = timeslotRepository.findAllActive();
        }

        return timeslots.stream()
                .map(this::toTimeslotResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TimeslotResponseDto getTimeslotById(UUID timeslotId) {
        log.info("Fetching timeslot with id: {}", timeslotId);
        Timeslot timeslot = timeslotRepository.findActiveById(timeslotId)
                .orElseThrow(() -> {
                    log.warn("No active timeslot with id {} found", timeslotId);
                    return new RuntimeException("No resource found with id: " + timeslotId);
                });
        return toTimeslotResponseDto(timeslot);
    }

    @Override
    @Transactional
    public TimeslotResponseDto updateTimeslot(UUID timeslotId, TimeslotRequestDto timeslotRequestDto) {
        log.info("Attempting to update timeslot with id: {}", timeslotId);
        Timeslot existingTimeslot = timeslotRepository.findActiveById(timeslotId)
                .orElseThrow(() -> {
                    log.warn("No active timeslot with id {} to update", timeslotId);
                    return new RuntimeException("No resource found to update with id: " + timeslotId);
                });

        // Best Practice: Validate logic, passing the existing UUID to exclude it from uniqueness check
        validateTimeslotLogic(timeslotRequestDto, timeslotId);

        existingTimeslot.setDayOfWeek(timeslotRequestDto.getDayOfWeek());
        existingTimeslot.setStartTime(timeslotRequestDto.getStartTime());
        existingTimeslot.setEndTime(timeslotRequestDto.getEndTime());
        existingTimeslot.setSlotLabel(timeslotRequestDto.getSlotLabel());
        existingTimeslot.setIsBreak(timeslotRequestDto.getIsBreak());

        Timeslot updatedTimeslot = timeslotRepository.save(existingTimeslot);
        log.info("Timeslot with id {} updated successfully", updatedTimeslot.getUuid());

        return toTimeslotResponseDto(updatedTimeslot);
    }

    @Override
    @Transactional
    public void deleteTimeslot(UUID timeslotId) {
        log.info("Attempting to soft delete timeslot with id: {}", timeslotId);
        if (!timeslotRepository.existsActiveById(timeslotId)) {
            log.warn("Failed to delete. Timeslot not found with id: {}", timeslotId);
            throw new RuntimeException("Timeslot id: " + timeslotId + " not found.");
        }

        timeslotRepository.softDeleteById(timeslotId);
        log.info("Timeslot with id {} marked as inactive successfully", timeslotId);
    }

    /**
     * Private helper to validate timeslot business logic.
     * @param dto The DTO to validate.
     * @param excludeUuid The UUID to exclude from uniqueness checks (null for creates, non-null for updates).
     */
    private void validateTimeslotLogic(TimeslotRequestDto dto, UUID excludeUuid) {
        // 1. Check if end time is after start time
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            log.warn("Validation failed: End time {} is not after start time {}", dto.getEndTime(), dto.getStartTime());
            throw new RuntimeException("End time must be after start time.");
        }

        // 2. Check for uniqueness (DayOfWeek + StartTime)
        boolean exists;
        if (excludeUuid == null) {
            // This is a CREATE operation
            exists = timeslotRepository.existsByDayOfWeekAndStartTime(dto.getDayOfWeek(), dto.getStartTime());
        } else {
            // This is an UPDATE operation
            exists = timeslotRepository.existsByDayOfWeekAndStartTimeAndUuidNot(dto.getDayOfWeek(), dto.getStartTime(), excludeUuid);
        }

        if (exists) {
            log.warn("Validation failed: A timeslot for day {} at {} already exists.", dto.getDayOfWeek(), dto.getStartTime());
            throw new RuntimeException("A timeslot for this day and start time already exists.");
        }
    }

    /**
     * Private helper to convert Timeslot Entity to Response DTO using builder.
     */
    private TimeslotResponseDto toTimeslotResponseDto(Timeslot entity) {
        if (entity == null) return null;
        return TimeslotResponseDto.builder()
                .uuid(entity.getUuid())
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .slotLabel(entity.getSlotLabel())
                .isBreak(entity.getIsBreak())
                .build();
    }
}