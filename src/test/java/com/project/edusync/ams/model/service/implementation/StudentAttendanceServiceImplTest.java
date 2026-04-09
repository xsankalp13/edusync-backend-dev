package com.project.edusync.ams.model.service.implementation;

import com.project.edusync.ams.model.dto.request.StudentAttendanceRequestDTO;
import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.entity.StudentDailyAttendance;
import com.project.edusync.ams.model.exception.AttendanceProcessingException;
import com.project.edusync.ams.model.repository.AbsenceDocumentationRepository;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceServiceImplTest {

    @Mock
    private StudentDailyAttendanceRepository studentRepo;

    @Mock
    private AttendanceTypeRepository attendanceTypeRepository;

    @Mock
    private AbsenceDocumentationRepository absenceDocumentationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StudentAttendanceServiceImpl service;

    @Test
    void markAttendanceBatch_rejectsOlderThanSevenDays() {
        AttendanceType presentType = attendanceType("P");
        when(attendanceTypeRepository.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(presentType));

        StudentAttendanceRequestDTO request = new StudentAttendanceRequestDTO(
                null,
                1L,
                "P",
                LocalDate.now().minusDays(8),
                null,
                null,
                "too old"
        );

        assertThrows(AttendanceProcessingException.class, () -> service.markAttendanceBatch(List.of(request), 7L));
        verify(studentRepo, never()).save(any(StudentDailyAttendance.class));
    }

    @Test
    void markAttendanceBatch_rejectsFutureDate() {
        AttendanceType presentType = attendanceType("P");
        when(attendanceTypeRepository.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(presentType));

        StudentAttendanceRequestDTO request = new StudentAttendanceRequestDTO(
                null,
                1L,
                "P",
                LocalDate.now().plusDays(1),
                null,
                null,
                "future"
        );

        assertThrows(AttendanceProcessingException.class, () -> service.markAttendanceBatch(List.of(request), 7L));
        verify(studentRepo, never()).save(any(StudentDailyAttendance.class));
    }

    @Test
    void markAttendanceBatch_allowsExactlySevenDaysBack() {
        AttendanceType presentType = attendanceType("P");
        LocalDate boundaryDate = LocalDate.now().minusDays(7);

        when(attendanceTypeRepository.findByShortCodeIgnoreCase("P")).thenReturn(Optional.of(presentType));
        when(studentRepo.findByStudentIdAndAttendanceDate(1L, boundaryDate)).thenReturn(Optional.empty());
        when(studentRepo.save(any(StudentDailyAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        StudentAttendanceRequestDTO request = new StudentAttendanceRequestDTO(
                null,
                1L,
                "P",
                boundaryDate,
                null,
                null,
                "boundary"
        );

        assertEquals(1, service.markAttendanceBatch(List.of(request), 7L).size());
        verify(studentRepo).save(any(StudentDailyAttendance.class));
    }

    @Test
    void updateAttendance_rejectsExistingRecordOlderThanSevenDays() {
        UUID recordUuid = UUID.randomUUID();
        StudentDailyAttendance existing = new StudentDailyAttendance();
        existing.setAttendanceDate(LocalDate.now().minusDays(8));

        when(studentRepo.findByUuid(recordUuid)).thenReturn(Optional.of(existing));

        StudentAttendanceRequestDTO request = new StudentAttendanceRequestDTO(
                null,
                1L,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(AttendanceProcessingException.class, () -> service.updateAttendance(recordUuid, request, 7L));
        verify(studentRepo, never()).save(any(StudentDailyAttendance.class));
    }

    @Test
    void updateAttendance_rejectsFutureAttendanceDateInRequest() {
        UUID recordUuid = UUID.randomUUID();
        StudentDailyAttendance existing = new StudentDailyAttendance();
        existing.setAttendanceDate(LocalDate.now());

        when(studentRepo.findByUuid(recordUuid)).thenReturn(Optional.of(existing));

        StudentAttendanceRequestDTO request = new StudentAttendanceRequestDTO(
                null,
                1L,
                null,
                LocalDate.now().plusDays(1),
                null,
                null,
                null
        );

        assertThrows(AttendanceProcessingException.class, () -> service.updateAttendance(recordUuid, request, 7L));
        verify(studentRepo, never()).save(any(StudentDailyAttendance.class));
    }

    private AttendanceType attendanceType(String shortCode) {
        AttendanceType type = new AttendanceType();
        type.setId(1L);
        type.setShortCode(shortCode);
        return type;
    }
}

