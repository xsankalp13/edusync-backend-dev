package com.project.edusync.em.model.service;

import com.project.edusync.adm.model.entity.Timeslot;
import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.em.model.enums.RoomAttendanceProgressStatus;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamControllerDashboardServiceTest {

    @Mock
    private SeatAllocationRepository seatAllocationRepository;
    @Mock
    private ExamScheduleRepository examScheduleRepository;

    @InjectMocks
    private ExamControllerDashboardService dashboardService;

    @Test
    void getDashboard_computesRoomAttendanceProgressAndTimer() {
        SeatAllocationRepository.ExamControllerRoomSummaryProjection notStarted = org.mockito.Mockito.mock(SeatAllocationRepository.ExamControllerRoomSummaryProjection.class);
        when(notStarted.getRoomId()).thenReturn(1L);
        when(notStarted.getRoomName()).thenReturn("A1");
        when(notStarted.getAllocatedCount()).thenReturn(25L);
        when(notStarted.getMarkedCount()).thenReturn(0L);

        SeatAllocationRepository.ExamControllerRoomSummaryProjection inProgress = org.mockito.Mockito.mock(SeatAllocationRepository.ExamControllerRoomSummaryProjection.class);
        when(inProgress.getRoomId()).thenReturn(2L);
        when(inProgress.getRoomName()).thenReturn("B1");
        when(inProgress.getAllocatedCount()).thenReturn(20L);
        when(inProgress.getMarkedCount()).thenReturn(7L);

        SeatAllocationRepository.ExamControllerRoomSummaryProjection completed = org.mockito.Mockito.mock(SeatAllocationRepository.ExamControllerRoomSummaryProjection.class);
        when(completed.getRoomId()).thenReturn(3L);
        when(completed.getRoomName()).thenReturn("C1");
        when(completed.getAllocatedCount()).thenReturn(15L);
        when(completed.getMarkedCount()).thenReturn(15L);

        when(seatAllocationRepository.findExamControllerRoomSummariesByExamId(99L))
            .thenReturn(List.of(notStarted, inProgress, completed));

        ExamSchedule schedule = new ExamSchedule();
        schedule.setExamDate(LocalDate.now());
        Timeslot timeslot = new Timeslot();
        timeslot.setStartTime(LocalTime.now().minusMinutes(10));
        timeslot.setEndTime(LocalTime.now().plusMinutes(50));
        schedule.setTimeslot(timeslot);

        when(examScheduleRepository.findByExamIdWithDetails(99L)).thenReturn(List.of(schedule));

        var response = dashboardService.getDashboard(99L);

        assertEquals(RoomAttendanceProgressStatus.NOT_STARTED, response.getRooms().get(0).getAttendanceStatus());
        assertEquals(RoomAttendanceProgressStatus.IN_PROGRESS, response.getRooms().get(1).getAttendanceStatus());
        assertEquals(RoomAttendanceProgressStatus.COMPLETED, response.getRooms().get(2).getAttendanceStatus());
        assertTrue(response.getTimer().getRemainingSeconds() > 0);
    }
}

