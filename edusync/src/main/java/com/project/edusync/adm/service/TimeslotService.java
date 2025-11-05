package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.request.TimeslotRequestDto;
import com.project.edusync.adm.model.dto.response.TimeslotResponseDto;

import java.util.List;
import java.util.UUID;

public interface TimeslotService {

    TimeslotResponseDto addTimeslot(TimeslotRequestDto timeslotRequestDto);

    /**
     * Retrieves all active timeslots.
     * @param dayOfWeek (Optional) Filter by day of the week (1-7).
     * @return A list of timeslots.
     */
    List<TimeslotResponseDto> getAllTimeslots(Short dayOfWeek);

    TimeslotResponseDto getTimeslotById(UUID timeslotId);

    TimeslotResponseDto updateTimeslot(UUID timeslotId, TimeslotRequestDto timeslotRequestDto);

    void deleteTimeslot(UUID timeslotId);
}