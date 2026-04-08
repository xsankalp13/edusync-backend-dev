package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.ResourceNotFoundException;
import com.project.edusync.adm.model.dto.response.AvailableSubjectDto;
import com.project.edusync.adm.model.dto.response.EditorContextResponseDto;
import com.project.edusync.adm.model.entity.CurriculumMap;
import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.repository.CurriculumMapRepository;
import com.project.edusync.adm.repository.ScheduleRepository;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.adm.repository.TimeslotRepository;
import com.project.edusync.adm.service.EditorContextService;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import com.project.edusync.uis.repository.details.TeacherDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditorContextServiceImpl implements EditorContextService {

    private final SectionRepository sectionRepository;
    private final TimeslotRepository timeslotRepository;
    private final ScheduleRepository scheduleRepository;
    private final CurriculumMapRepository curriculumMapRepository;
    private final TeacherDetailsRepository teacherDetailsRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "editorContext", key = "#sectionId")
    public EditorContextResponseDto getEditorContext(UUID sectionId) {
        Section section = sectionRepository.findByIdWithClassTeacher(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("No section resource found with id: " + sectionId));

        List<Timeslot> timeslots = timeslotRepository.findAllActive();
        List<Schedule> existingSchedule = scheduleRepository.findAllActiveWithReferencesBySectionUuid(sectionId);
        List<TeacherDetails> teachers = teacherDetailsRepository.findAllActiveWithSubjects();

        // Resolve class teacher name and timetable teacher ID (if assigned)
        String classTeacherName = null;
        String classTeacherId = null;
        UUID classTeacherStaffUuid = null;
        if (section.getClassTeacher() != null) {
            var ct = section.getClassTeacher();
            classTeacherStaffUuid = ct.getUuid();
            if (ct.getUserProfile() != null) {
                classTeacherName = buildName(
                        ct.getUserProfile().getFirstName(),
                        ct.getUserProfile().getLastName());
            }
            // Map Staff → TeacherDetails to get the ID used in schedule entries
            classTeacherId = teacherDetailsRepository.findByStaff_Id(ct.getId())
                    .map(td -> String.valueOf(td.getId()))
                    .orElse(null);
        }

        return EditorContextResponseDto.builder()
                .section(EditorContextResponseDto.SectionSummaryDto.builder()
                        .uuid(section.getUuid())
                        .sectionName(section.getSectionName())
                        .className(section.getAcademicClass().getName())
                        .classTeacherName(classTeacherName)
                        .classTeacherId(classTeacherId)
                        .classTeacherStaffUuid(classTeacherStaffUuid)
                        .build())
                .timeslots(timeslots.stream().map(this::toTimeslotItem).toList())
                .availableSubjects(extractSubjects(section))
                .teachers(buildTeachers(teachers))
                .existingSchedule(existingSchedule.stream().map(this::toExistingScheduleItem).toList())
                .build();
    }

    private EditorContextResponseDto.TimeslotItemDto toTimeslotItem(Timeslot timeslot) {
        return EditorContextResponseDto.TimeslotItemDto.builder()
                .uuid(timeslot.getUuid())
                .dayOfWeek(timeslot.getDayOfWeek())
                .startTime(timeslot.getStartTime())
                .endTime(timeslot.getEndTime())
                .slotLabel(timeslot.getSlotLabel())
                .isBreak(timeslot.getIsBreak())
                .build();
    }

    private List<AvailableSubjectDto> extractSubjects(Section section) {
        return curriculumMapRepository.findActiveByClassUuid(section.getAcademicClass().getUuid()).stream()
                .map(CurriculumMap::getSubject)
                .filter(s -> s != null && Boolean.TRUE.equals(s.getIsActive()))
                .map(this::toAvailableSubject)
                .toList();
    }

    private AvailableSubjectDto toAvailableSubject(Subject subject) {
        return AvailableSubjectDto.builder()
                .uuid(subject.getUuid())
                .name(subject.getName())
                .subjectCode(subject.getSubjectCode())
                .color(subject.getColor())
                .build();
    }

    private List<EditorContextResponseDto.TeacherItemDto> buildTeachers(List<TeacherDetails> teachers) {
        return teachers.stream()
                .map(teacher -> {
                    String firstName = "";
                    String lastName = "";
                    
                    if (teacher.getStaff() != null && teacher.getStaff().getUserProfile() != null) {
                        firstName = teacher.getStaff().getUserProfile().getFirstName();
                        lastName = teacher.getStaff().getUserProfile().getLastName();
                    }

                    return EditorContextResponseDto.TeacherItemDto.builder()
                            .id(String.valueOf(teacher.getId()))
                            .name(buildName(firstName, lastName))
                            .teachableSubjectIds(teacher.getTeachableSubjects().stream()
                                    .filter(Objects::nonNull)
                                    .map(Subject::getUuid)
                                    .filter(Objects::nonNull)
                                    .toList())
                            .build();
                })
                .toList();
    }

    private EditorContextResponseDto.ExistingScheduleItemDto toExistingScheduleItem(Schedule schedule) {
        return EditorContextResponseDto.ExistingScheduleItemDto.builder()
                .uuid(schedule.getUuid())
                .subjectId(schedule.getSubject() != null ? schedule.getSubject().getUuid() : null)
                .teacherId(schedule.getTeacher() != null ? String.valueOf(schedule.getTeacher().getId()) : null)
                .roomId(schedule.getRoom() != null ? schedule.getRoom().getUuid() : null)
                .timeslotId(schedule.getTimeslot() != null ? schedule.getTimeslot().getUuid() : null)
                .slotLabel(schedule.getTimeslot() != null ? schedule.getTimeslot().getSlotLabel() : null)
                .build();
    }

    private String buildName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName;
        String last = lastName == null ? "" : lastName;
        return (first + " " + last).trim();
    }
}


