package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.response.AvailableRoomDto;
import com.project.edusync.adm.model.dto.response.AvailableSubjectDto;
import com.project.edusync.adm.model.dto.response.AvailableTeacherDto;
import com.project.edusync.adm.model.entity.CurriculumMap;
import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.repository.CurriculumMapRepository;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.service.DataFetchService;
import com.project.edusync.uis.model.entity.details.TeacherDetails; // Assumed path
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true) // Service is read-only by default
public class DataFetchServiceImpl implements DataFetchService {

    private final com.project.edusync.uis.repository.details.TeacherDetailsRepository teacherDetailsRepository;
    private final RoomRepository roomRepository;
    private final SectionRepository sectionRepository;
    private final CurriculumMapRepository curriculumMapRepository;

    @Override
    @Cacheable(value = "availableTeachers", key = "{#subjectId, #timeslotId}")
    public List<AvailableTeacherDto> getAvailableTeachers(UUID subjectId, UUID timeslotId) {
        log.info("Fetching qualified and available teachers for subject {} at timeslot {}", subjectId, timeslotId);

        List<TeacherDetails> teachers;
        if (timeslotId != null) {
            teachers = teacherDetailsRepository.findAvailableTeachersForSlot(subjectId, timeslotId);
        } else {
            teachers = teacherDetailsRepository.findQualifiedTeachersForSubject(subjectId);
        }

        return teachers.stream()
                .map(this::toAvailableTeacherDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailableRoomDto> getAvailableRooms(UUID timeslotId, String roomType) {
        log.info("Fetching available rooms for timeslot {} (Type: {})", timeslotId, roomType);
        List<Room> rooms;
        if (roomType != null && !roomType.isEmpty()) {
            rooms = roomRepository.findAvailableRoomsByType(timeslotId, roomType);
        } else {
            rooms = roomRepository.findAvailableRooms(timeslotId);
        }

        return rooms.stream()
                .map(this::toAvailableRoomDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "availableSubjects", key = "#sectionId")
    public List<AvailableSubjectDto> getAvailableSubjects(UUID sectionId) {
        log.info("Fetching available subjects for section {}", sectionId);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> {
                    log.warn("Section with id {} not found", sectionId);
                    return new ResourceNotFoundException("No section resource found with id: " + sectionId);
                });

        List<CurriculumMap> curriculumMaps = curriculumMapRepository.findActiveByClassUuid(section.getAcademicClass().getUuid());

        return curriculumMaps.stream()
                .map(CurriculumMap::getSubject)
                .filter(subject -> subject != null && Boolean.TRUE.equals(subject.getIsActive()))
                .distinct()
                .map(this::toAvailableSubjectDto)
                .collect(Collectors.toList());
    }

    // --- Private DTO Builder Methods ---

    private AvailableTeacherDto toAvailableTeacherDto(TeacherDetails entity) {
        return AvailableTeacherDto.builder()
                .id(entity.getId()) // Using Long id
                .name(entity.getStaff().getUserProfile().getFirstName()) // Assumed path to name
                .build();
    }

    private AvailableRoomDto toAvailableRoomDto(Room entity) {
        return AvailableRoomDto.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .build();
    }

    private AvailableSubjectDto toAvailableSubjectDto(Subject entity) {
        return AvailableSubjectDto.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .subjectCode(entity.getSubjectCode())
                .color(entity.getColor())
                .build();
    }
}