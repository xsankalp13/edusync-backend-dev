package com.project.edusync.teacher.service;

import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherScheduleService {

    private final ScheduleService scheduleService;

    public List<ScheduleResponseDto> getScheduleForSection(UUID sectionId) {
        return scheduleService.getScheduleForSection(sectionId);
    }

    public List<ScheduleResponseDto> getScheduleForTeacher(Long staffId) {
        return scheduleService.getScheduleForTeacher(staffId);
    }
}
