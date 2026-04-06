package com.project.edusync.hrms.service;

import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventCreateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventResponseDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventUpdateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarSummaryDTO;

import java.util.List;

public interface AcademicCalendarService {

    List<CalendarEventResponseDTO> listEvents(String academicYear, Integer month);

    CalendarEventResponseDTO createEvent(CalendarEventCreateDTO dto);

    BulkOperationResultDTO bulkCreateEvents(List<CalendarEventCreateDTO> dtos);

    CalendarEventResponseDTO updateEvent(Long eventId, CalendarEventUpdateDTO dto);

    CalendarEventResponseDTO updateEventByIdentifier(String identifier, CalendarEventUpdateDTO dto);

    void deleteEvent(Long eventId);

    void deleteEventByIdentifier(String identifier);

    CalendarSummaryDTO getSummary(String academicYear);
}

