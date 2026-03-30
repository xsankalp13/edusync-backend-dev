package com.project.edusync.adm.service.impl;

import com.project.edusync.adm.exception.InvalidRequestException;
import com.project.edusync.adm.model.dto.request.ScheduleRequestDto;
import com.project.edusync.adm.model.dto.response.ScheduleResponseDto;
import com.project.edusync.adm.model.entity.*;
import com.project.edusync.adm.repository.*;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.model.entity.details.TeacherDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private com.project.edusync.uis.repository.details.TeacherDetailsRepository teacherDetailsRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TimeslotRepository timeslotRepository;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    void addSchedule_inheritsSectionDefaultRoom_whenRoomIdIsNull() {
        UUID sectionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Long teacherId = 21L;
        UUID timeslotId = UUID.randomUUID();
        UUID defaultRoomId = UUID.randomUUID();

        ScheduleRequestDto dto = new ScheduleRequestDto();
        dto.setSectionId(sectionId);
        dto.setSubjectId(subjectId);
        dto.setTeacherId(teacherId);
        dto.setRoomId(null);
        dto.setTimeslotId(timeslotId);

        AcademicClass academicClass = new AcademicClass();
        academicClass.setName("Class 10");

        Room defaultRoom = new Room();
        defaultRoom.setUuid(defaultRoomId);
        defaultRoom.setName("Room 101");
        defaultRoom.setRoomType("CLASSROOM");
        defaultRoom.setIsActive(true);

        Section section = new Section();
        section.setUuid(sectionId);
        section.setSectionName("A");
        section.setAcademicClass(academicClass);
        section.setDefaultRoom(defaultRoom);

        Subject subject = new Subject();
        subject.setUuid(subjectId);
        subject.setName("Mathematics");
        subject.setSubjectCode("MATH-101");

        UserProfile userProfile = new UserProfile();
        userProfile.setFirstName("Aditi");

        Staff staff = new Staff();
        staff.setId(300L);
        staff.setUserProfile(userProfile);

        TeacherDetails teacherDetails = new TeacherDetails();
        teacherDetails.setStaff(staff);

        Timeslot timeslot = new Timeslot();
        timeslot.setUuid(timeslotId);
        timeslot.setSlotLabel("P1");
        timeslot.setDayOfWeek((short) 1);
        timeslot.setStartTime(LocalTime.of(8, 0));
        timeslot.setEndTime(LocalTime.of(8, 45));

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(subjectRepository.findActiveById(subjectId)).thenReturn(Optional.of(subject));
        when(teacherDetailsRepository.findActiveById(teacherId)).thenReturn(Optional.of(teacherDetails));
        when(roomRepository.findActiveById(defaultRoomId)).thenReturn(Optional.of(defaultRoom));
        when(timeslotRepository.findActiveById(timeslotId)).thenReturn(Optional.of(timeslot));

        when(scheduleRepository.findTeacherConflict(teacherId, timeslotId, null, null)).thenReturn(Optional.empty());
        when(scheduleRepository.findRoomConflict(defaultRoomId, timeslotId, null, null)).thenReturn(Optional.empty());
        when(scheduleRepository.findSectionConflict(sectionId, timeslotId, null, null)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule s = invocation.getArgument(0);
            s.setUuid(UUID.randomUUID());
            return s;
        });

        ScheduleResponseDto response = scheduleService.addSchedule(dto);

        assertEquals(defaultRoomId, response.getRoom().getUuid());
        assertEquals(defaultRoomId, dto.getRoomId());
        verify(roomRepository).findActiveById(defaultRoomId);
    }

    @Test
    void addSchedule_throwsBadRequest_whenRoomIdIsNullAndSectionHasNoDefaultRoom() {
        UUID sectionId = UUID.randomUUID();

        ScheduleRequestDto dto = new ScheduleRequestDto();
        dto.setSectionId(sectionId);
        dto.setSubjectId(UUID.randomUUID());
        dto.setTeacherId(11L);
        dto.setRoomId(null);
        dto.setTimeslotId(UUID.randomUUID());

        Section section = new Section();
        section.setUuid(sectionId);
        section.setSectionName("B");
        section.setDefaultRoom(null);

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

        assertThrows(InvalidRequestException.class, () -> scheduleService.addSchedule(dto));
    }
}

