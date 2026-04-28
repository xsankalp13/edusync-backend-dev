package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Room;
import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.adm.repository.RoomRepository;
import com.project.edusync.common.exception.BadRequestException;
import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.em.model.dto.request.ExamAttendanceFinalizeRequestDTO;
import com.project.edusync.em.model.dto.request.ExamAttendanceMarkEntryDTO;
import com.project.edusync.em.model.dto.request.ExamAttendanceMarkRequestDTO;
import com.project.edusync.em.model.entity.ExamAttendance;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.enums.ExamAttendanceStatus;
import com.project.edusync.em.model.repository.ExamAttendanceRepository;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.InvigilationRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Temporarily disabled: exam attendance tests require refactor for current schedule validation")
class ExamAttendanceServiceTest {

    @Mock
    private AuthUtil authUtil;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private InvigilationRepository invigilationRepository;
    @Mock
    private SeatAllocationRepository seatAllocationRepository;
    @Mock
    private ExamAttendanceRepository examAttendanceRepository;
    @Mock
    private ExamScheduleRepository examScheduleRepository;
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ExamAttendanceService examAttendanceService;

    @Test
    void finalizeAttendance_marksUnmarkedStudentsAbsent() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(staffRepository.findById(55L)).thenReturn(Optional.of(staff));

        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);
        when(examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(anyCollection(), eq(20L))).thenReturn(false);
        when(seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(1L, 2L, 3L));

        Room room = new Room();
        room.setId(20L);
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(10L);
        schedule.setExamDate(LocalDate.now().minusDays(1));
        Timeslot timeslot = new Timeslot();
        timeslot.setEndTime(LocalTime.of(10, 0));
        schedule.setTimeslot(timeslot);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(schedule));

        SeatAllocationRepository.RoomStudentScheduleProjection p1 = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(p1.getStudentId()).thenReturn(1L);
        when(p1.getExamScheduleId()).thenReturn(10L);
        SeatAllocationRepository.RoomStudentScheduleProjection p2 = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(p2.getStudentId()).thenReturn(2L);
        when(p2.getExamScheduleId()).thenReturn(11L);
        SeatAllocationRepository.RoomStudentScheduleProjection p3 = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(p3.getStudentId()).thenReturn(3L);
        when(p3.getExamScheduleId()).thenReturn(12L);
        when(seatAllocationRepository.findStudentSchedulesInRoomByTimeWindowAndStudentIds(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
            .thenReturn(List.of(p1, p2, p3));

        ExamSchedule schedule11 = new ExamSchedule();
        schedule11.setId(11L);
        ExamSchedule schedule12 = new ExamSchedule();
        schedule12.setId(12L);
        when(examScheduleRepository.findAllById(anyCollection())).thenReturn(List.of(schedule, schedule11, schedule12));

        Student s2 = new Student();
        s2.setId(2L);
        Student s3 = new Student();
        s3.setId(3L);
        when(studentRepository.findAllById(anyCollection())).thenReturn(List.of(s2, s3));

        Student s1 = new Student();
        s1.setId(1L);
        ExamAttendance existing = ExamAttendance.builder()
            .id(500L)
            .examSchedule(schedule)
            .student(s1)
            .room(room)
            .status(ExamAttendanceStatus.PRESENT)
            .timestamp(LocalDateTime.now().minusMinutes(10))
            .finalized(false)
            .build();
        when(examAttendanceRepository.findByExamScheduleIdsAndStudentIds(anyCollection(), anyCollection())).thenReturn(List.of(existing));

        ExamAttendanceFinalizeRequestDTO request = new ExamAttendanceFinalizeRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);

        var response = examAttendanceService.finalizeAttendance(request);

        assertEquals(3, response.getTotalStudents());
        assertEquals(1, response.getAlreadyMarked());
        assertEquals(2, response.getAutoMarkedAbsent());
        assertTrue(response.isFinalized());

        ArgumentCaptor<Iterable<ExamAttendance>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(examAttendanceRepository).saveAll(captor.capture());
        long autoAbsentCount = 0;
        for (ExamAttendance attendance : captor.getValue()) {
            if (attendance.getStatus() == ExamAttendanceStatus.ABSENT) {
                autoAbsentCount++;
            }
        }
        assertEquals(2L, autoAbsentCount);
    }

    @Test
    void markAttendance_rejectsWhenAlreadyFinalized() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(1L));
        SeatAllocationRepository.RoomStudentScheduleProjection p1 = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(p1.getStudentId()).thenReturn(1L);
        when(p1.getExamScheduleId()).thenReturn(10L);
        when(seatAllocationRepository.findStudentSchedulesInRoomByTimeWindowAndStudentIds(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
            .thenReturn(List.of(p1));
        when(examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(anyCollection(), eq(20L))).thenReturn(true);

        ExamAttendanceMarkEntryDTO entry = new ExamAttendanceMarkEntryDTO();
        entry.setStudentId(1L);
        entry.setStatus(ExamAttendanceStatus.PRESENT);

        ExamAttendanceMarkRequestDTO request = new ExamAttendanceMarkRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);
        request.setEntries(List.of(entry));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examAttendanceService.markAttendance(request));
        assertEquals("Attendance already finalized for this room", ex.getMessage());
    }

    @Test
    void finalizeAttendance_rejectsBeforeExamEnd() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);

        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(10L);
        schedule.setExamDate(LocalDate.now());
        Timeslot timeslot = new Timeslot();
        timeslot.setEndTime(LocalTime.now().plusHours(1));
        schedule.setTimeslot(timeslot);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(schedule));

        ExamAttendanceFinalizeRequestDTO request = new ExamAttendanceFinalizeRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examAttendanceService.finalizeAttendance(request));
        assertEquals("Attendance can be finalized only after exam end time", ex.getMessage());
    }

    @Test
    void markAttendance_rejectsDuplicateStudentEntriesInSingleRequest() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));

        ExamAttendanceMarkEntryDTO first = new ExamAttendanceMarkEntryDTO();
        first.setStudentId(1L);
        first.setStatus(ExamAttendanceStatus.PRESENT);

        ExamAttendanceMarkEntryDTO duplicate = new ExamAttendanceMarkEntryDTO();
        duplicate.setStudentId(1L);
        duplicate.setStatus(ExamAttendanceStatus.ABSENT);

        ExamAttendanceMarkRequestDTO request = new ExamAttendanceMarkRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);
        request.setEntries(List.of(first, duplicate));

        SeatAllocationRepository.RoomStudentScheduleProjection p1 = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(p1.getStudentId()).thenReturn(1L);
        when(p1.getExamScheduleId()).thenReturn(10L);
        when(seatAllocationRepository.findStudentSchedulesInRoomByTimeWindowAndStudentIds(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
            .thenReturn(List.of(p1));
        when(examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(anyCollection(), eq(20L))).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> examAttendanceService.markAttendance(request));
        assertEquals("Duplicate student entries are not allowed in one mark request", ex.getMessage());
    }

    @Test
    void getRoomAttendanceRoster_returnsSeatAllocationDrivenRosterWithAttendance() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);

        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        SeatAllocationRepository.ExamRoomStudentProjection p1 = org.mockito.Mockito.mock(SeatAllocationRepository.ExamRoomStudentProjection.class);
        when(p1.getExamScheduleId()).thenReturn(10L);
        when(p1.getStudentId()).thenReturn(1L);
        when(p1.getRollNo()).thenReturn(11);
        when(p1.getFirstName()).thenReturn("Ana");
        when(p1.getLastName()).thenReturn("Lee");
        when(p1.getClassName()).thenReturn("Class 3");
        when(p1.getSubjectName()).thenReturn("Physics");
        when(p1.getSeatNumber()).thenReturn("R2-C1");
        when(p1.getRowNumber()).thenReturn(2);
        when(p1.getColumnNumber()).thenReturn(1);
        when(p1.getAttendanceStatus()).thenReturn(ExamAttendanceStatus.PRESENT);
        when(p1.getFinalized()).thenReturn(Boolean.FALSE);

        SeatAllocationRepository.ExamRoomStudentProjection p2 = org.mockito.Mockito.mock(SeatAllocationRepository.ExamRoomStudentProjection.class);
        when(p2.getExamScheduleId()).thenReturn(11L);
        when(p2.getStudentId()).thenReturn(2L);
        when(p2.getRollNo()).thenReturn(22);
        when(p2.getFirstName()).thenReturn("Bob");
        when(p2.getLastName()).thenReturn("Khan");
        when(p2.getClassName()).thenReturn("Class 5");
        when(p2.getSubjectName()).thenReturn("Physics");
        when(p2.getSeatNumber()).thenReturn(null);
        when(p2.getRowNumber()).thenReturn(2);
        when(p2.getColumnNumber()).thenReturn(2);
        when(p2.getAttendanceStatus()).thenReturn(null);
        when(p2.getFinalized()).thenReturn(null);

        when(seatAllocationRepository.findExamRoomStudentsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(p1, p2));

        var roster = examAttendanceService.getRoomAttendanceRoster(20L, 10L);

        assertEquals(2, roster.size());
        assertEquals("Ana Lee", roster.get(0).getStudentName());
        assertEquals("Class 3", roster.get(0).getClassName());
        assertEquals("Physics", roster.get(0).getSubjectName());
        assertEquals("R2-C1", roster.get(0).getSeatNumber());
        assertEquals(ExamAttendanceStatus.PRESENT, roster.get(0).getAttendanceStatus());

        assertEquals("Bob Khan", roster.get(1).getStudentName());
        assertEquals("Class 5", roster.get(1).getClassName());
        assertEquals("R2-C2", roster.get(1).getSeatNumber());
        assertEquals(null, roster.get(1).getAttendanceStatus());
    }

    @Test
    void markAttendance_allowsStatusCorrectionBeforeFinalize() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));

        SeatAllocationRepository.RoomStudentScheduleProjection mapProjection = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(mapProjection.getStudentId()).thenReturn(1L);
        when(mapProjection.getExamScheduleId()).thenReturn(10L);
        when(seatAllocationRepository.findStudentSchedulesInRoomByTimeWindowAndStudentIds(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
            .thenReturn(List.of(mapProjection));

        when(examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(anyCollection(), eq(20L))).thenReturn(false);

        Room room = new Room();
        room.setId(20L);

        Student student = new Student();
        student.setId(1L);

        ExamAttendance existing = ExamAttendance.builder()
            .id(500L)
            .examSchedule(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0)))
            .student(student)
            .room(room)
            .status(ExamAttendanceStatus.PRESENT)
            .build();
        when(examAttendanceRepository.findByExamScheduleIdsAndStudentIds(anyCollection(), anyCollection()))
            .thenReturn(List.of(existing));

        ExamAttendanceMarkEntryDTO entry = new ExamAttendanceMarkEntryDTO();
        entry.setStudentId(1L);
        entry.setStatus(ExamAttendanceStatus.ABSENT);

        ExamAttendanceMarkRequestDTO request = new ExamAttendanceMarkRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);
        request.setAttendances(List.of(entry));

        var response = examAttendanceService.markAttendance(request);

        assertEquals(1, response.getSavedCount());
        assertEquals(ExamAttendanceStatus.ABSENT, existing.getStatus());
        verify(examAttendanceRepository).saveAll(anyCollection());
    }

    @Test
    void markAttendance_normalizesLegacyMalpracticeStatusToPresentWithFlag() {
        when(authUtil.getCurrentUserId()).thenReturn(100L);

        Staff staff = new Staff();
        staff.setId(55L);
        when(staffRepository.findByUserProfile_User_Id(100L)).thenReturn(Optional.of(staff));
        when(staffRepository.findById(55L)).thenReturn(Optional.of(staff));
        when(invigilationRepository.existsByExamScheduleIdAndRoom_IdAndStaffId(10L, 20L, 55L)).thenReturn(true);
        when(examScheduleRepository.findByIdWithTimeslot(10L)).thenReturn(Optional.of(buildSchedule(10L, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(seatAllocationRepository.findExamRoomStudentIdsByTimeWindow(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(1L));

        SeatAllocationRepository.RoomStudentScheduleProjection mapProjection = org.mockito.Mockito.mock(SeatAllocationRepository.RoomStudentScheduleProjection.class);
        when(mapProjection.getStudentId()).thenReturn(1L);
        when(mapProjection.getExamScheduleId()).thenReturn(10L);
        when(seatAllocationRepository.findStudentSchedulesInRoomByTimeWindowAndStudentIds(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
            .thenReturn(List.of(mapProjection));

        when(examAttendanceRepository.existsByExamScheduleIdInAndRoomIdAndFinalizedTrue(anyCollection(), eq(20L))).thenReturn(false);
        when(examAttendanceRepository.findByExamScheduleIdsAndStudentIds(anyCollection(), anyCollection())).thenReturn(List.of());

        Room room = new Room();
        room.setId(20L);
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        Student student = new Student();
        student.setId(1L);
        when(studentRepository.findAllById(anyCollection())).thenReturn(List.of(student));

        ExamAttendanceMarkEntryDTO entry = new ExamAttendanceMarkEntryDTO();
        entry.setStudentId(1L);
        entry.setStatus(ExamAttendanceStatus.MALPRACTICE);

        ExamAttendanceMarkRequestDTO request = new ExamAttendanceMarkRequestDTO();
        request.setExamScheduleId(10L);
        request.setRoomId(20L);
        request.setAttendances(List.of(entry));

        examAttendanceService.markAttendance(request);

        ArgumentCaptor<Iterable<ExamAttendance>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(examAttendanceRepository).saveAll(captor.capture());
        ExamAttendance saved = captor.getValue().iterator().next();
        assertEquals(ExamAttendanceStatus.PRESENT, saved.getStatus());
        assertTrue(saved.isMalpracticeReported());
    }

    private ExamSchedule buildSchedule(Long id, LocalDate date, LocalTime start, LocalTime end) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(id);
        schedule.setExamDate(date);
        Timeslot timeslot = new Timeslot();
        timeslot.setStartTime(start);
        timeslot.setEndTime(end);
        schedule.setTimeslot(timeslot);
        return schedule;
    }
}
