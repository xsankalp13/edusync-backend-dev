package com.project.edusync.ams.model.service;

import com.project.edusync.ams.model.dto.response.DailyRosterResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface DailyRosterService {
    List<DailyRosterResponseDTO> getDailyRoster(Long classId, LocalDate date);
}