package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.hrms.dto.calendar.CalendarEventCreateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarSummaryDTO;
import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarServiceImplTest {

    @Mock
    private AcademicCalendarEventRepository calendarEventRepository;

    @InjectMocks
    private AcademicCalendarServiceImpl service;

    @Test
    void listEventsRequiresAcademicYearWhenMonthProvided() {
        assertThrows(EdusyncException.class, () -> service.listEvents(null, 4));
    }

    @Test
    void createEventThrowsConflictWhenDateAlreadyExists() {
        CalendarEventCreateDTO dto = new CalendarEventCreateDTO(
                "2025-2026",
                LocalDate.of(2025, 10, 2),
                DayType.HOLIDAY,
                "Gandhi Jayanti",
                null,
                true,
                true
        );

        when(calendarEventRepository.existsByAcademicYearAndDateAndIsActiveTrue("2025-2026", LocalDate.of(2025, 10, 2)))
                .thenReturn(true);

        assertThrows(EdusyncException.class, () -> service.createEvent(dto));
    }

    @Test
    void summaryUsesDefaultWeekdayWeekendLogicWhenNoOverrides() {
        when(calendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2025-2026",
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of());

        CalendarSummaryDTO summary = service.getSummary("2025-2026");

        assertEquals("2025-2026", summary.academicYear());
        assertEquals(365, summary.totalWorkingDays() + summary.totalHolidays() + summary.totalHalfDays());
    }

    @Test
    void summaryCountsHalfDayOverrideSeparately() {
        AcademicCalendarEvent halfDay = new AcademicCalendarEvent();
        halfDay.setAcademicYear("2025-2026");
        halfDay.setDate(LocalDate.of(2025, 4, 2));
        halfDay.setDayType(DayType.HALF_DAY);

        when(calendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                "2025-2026",
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of(halfDay));

        CalendarSummaryDTO summary = service.getSummary("2025-2026");

        assertEquals(1, summary.totalHalfDays());
    }
}
