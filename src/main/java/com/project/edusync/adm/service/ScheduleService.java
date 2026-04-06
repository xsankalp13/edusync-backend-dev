package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.dto.response.TimetableOverviewResponseDto;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<ScheduleResponseDto> getScheduleForSection(UUID sectionId);

    List<ScheduleResponseDto> getScheduleForTeacher(Long staffId);

    List<TimetableOverviewResponseDto> getScheduleOverview();

    ScheduleResponseDto addSchedule(ScheduleRequestDto scheduleRequestDto);

    ScheduleResponseDto updateSchedule(UUID scheduleId, ScheduleRequestDto scheduleRequestDto);

    void deleteSchedule(UUID scheduleId);

    void saveAsDraft(UUID sectionId, String statusType);

    List<ScheduleResponseDto> replaceSectionScheduleBulk(UUID sectionId, List<ScheduleRequestDto> schedules);

    void deleteScheduleBySection(UUID sectionId);
}
