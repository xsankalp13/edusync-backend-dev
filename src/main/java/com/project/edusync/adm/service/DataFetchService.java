package com.project.edusync.adm.service;

import com.project.edusync.adm.model.dto.response.AvailableRoomDto;
import com.project.edusync.adm.model.dto.response.AvailableSubjectDto;
import com.project.edusync.adm.model.dto.response.AvailableTeacherDto;

import java.util.List;
import java.util.UUID;

public interface DataFetchService {

    /**
     * Finds teachers qualified for a subject and available at a timeslot.
     */
    List<AvailableTeacherDto> getAvailableTeachers(UUID subjectId, UUID timeslotId);

    /**
     * Finds rooms available at a timeslot, optionally filtered by room type.
     */
    List<AvailableRoomDto> getAvailableRooms(UUID timeslotId, String roomType);

    /**
     * Finds subjects assigned to a specific section (via constraints).
     */
    List<AvailableSubjectDto> getAvailableSubjects(UUID sectionId);
}