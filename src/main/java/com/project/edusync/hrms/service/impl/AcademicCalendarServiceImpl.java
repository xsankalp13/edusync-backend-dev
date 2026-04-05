package com.project.edusync.hrms.service.impl;

import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.common.exception.ResourceNotFoundException;
import com.project.edusync.common.utils.PublicIdentifierResolver;
import com.project.edusync.hrms.dto.calendar.BulkOperationResultDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventCreateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventResponseDTO;
import com.project.edusync.hrms.dto.calendar.CalendarEventUpdateDTO;
import com.project.edusync.hrms.dto.calendar.CalendarSummaryDTO;
import com.project.edusync.hrms.dto.calendar.MonthSummaryDTO;
import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import com.project.edusync.hrms.model.enums.DayType;
import com.project.edusync.hrms.repository.AcademicCalendarEventRepository;
import com.project.edusync.hrms.service.AcademicCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcademicCalendarServiceImpl implements AcademicCalendarService {

    private final AcademicCalendarEventRepository calendarEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventResponseDTO> listEvents(String academicYear, Integer month) {
        if (academicYear == null && month != null) {
            throw new EdusyncException("academicYear is required when month is provided", HttpStatus.BAD_REQUEST);
        }

        if (academicYear != null && month != null) {
            validateMonth(month);
            LocalDate monthStart = LocalDate.of(resolveCalendarYear(academicYear, month), month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            return calendarEventRepository
                    .findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(academicYear, monthStart, monthEnd)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (academicYear != null) {
            return calendarEventRepository.findByAcademicYearAndIsActiveTrueOrderByDateAsc(academicYear)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return calendarEventRepository.findByIsActiveTrueOrderByDateAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CalendarEventResponseDTO createEvent(CalendarEventCreateDTO dto) {
        if (calendarEventRepository.existsByAcademicYearAndDateAndIsActiveTrue(dto.academicYear(), dto.date())) {
            throw new EdusyncException("Calendar event already exists for date " + dto.date(), HttpStatus.CONFLICT);
        }

        AcademicCalendarEvent event = new AcademicCalendarEvent();
        mapCreateOrUpdate(dto.academicYear(), dto.date(), dto.dayType(), dto.title(), dto.description(), dto.appliesToStaff(), dto.appliesToStudents(), event);
        return toResponse(calendarEventRepository.save(event));
    }

    @Override
    @Transactional
    public BulkOperationResultDTO bulkCreateEvents(List<CalendarEventCreateDTO> dtos) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < dtos.size(); i++) {
            CalendarEventCreateDTO dto = dtos.get(i);
            try {
                createEvent(dto);
                successCount++;
            } catch (Exception ex) {
                errors.add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }

        return new BulkOperationResultDTO(dtos.size(), successCount, dtos.size() - successCount, errors);
    }

    @Override
    @Transactional
    public CalendarEventResponseDTO updateEvent(Long eventId, CalendarEventUpdateDTO dto) {
        AcademicCalendarEvent event = findById(eventId);
        if (calendarEventRepository.existsByAcademicYearAndDateAndIsActiveTrueAndIdNot(dto.academicYear(), dto.date(), eventId)) {
            throw new EdusyncException("Calendar event already exists for date " + dto.date(), HttpStatus.CONFLICT);
        }

        mapCreateOrUpdate(dto.academicYear(), dto.date(), dto.dayType(), dto.title(), dto.description(), dto.appliesToStaff(), dto.appliesToStudents(), event);
        return toResponse(calendarEventRepository.save(event));
    }

    @Override
    @Transactional
    public CalendarEventResponseDTO updateEventByIdentifier(String identifier, CalendarEventUpdateDTO dto) {
        AcademicCalendarEvent event = findByIdentifier(identifier);
        return updateEvent(event.getId(), dto);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        AcademicCalendarEvent event = findById(eventId);
        event.setActive(false);
        calendarEventRepository.save(event);
    }

    @Override
    @Transactional
    public void deleteEventByIdentifier(String identifier) {
        AcademicCalendarEvent event = findByIdentifier(identifier);
        deleteEvent(event.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarSummaryDTO getSummary(String academicYear) {
        YearRange yearRange = parseAcademicYear(academicYear);
        LocalDate start = LocalDate.of(yearRange.startYear(), Month.APRIL, 1);
        LocalDate end = LocalDate.of(yearRange.endYear(), Month.MARCH, 31);

        Map<LocalDate, DayType> overrideMap = calendarEventRepository.findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
                        academicYear,
                        start,
                        end
                ).stream()
                .collect(java.util.stream.Collectors.toMap(AcademicCalendarEvent::getDate, AcademicCalendarEvent::getDayType));

        Map<Integer, Counter> monthlyCounter = new HashMap<>();
        int totalWorking = 0;
        int totalHoliday = 0;
        int totalHalfDay = 0;

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            DayType effectiveType = overrideMap.getOrDefault(cursor, defaultDayType(cursor));
            Counter counter = monthlyCounter.computeIfAbsent(cursor.getMonthValue(), k -> new Counter());

            if (effectiveType == DayType.HALF_DAY) {
                counter.halfDays++;
                totalHalfDay++;
            } else if (effectiveType == DayType.WORKING || effectiveType == DayType.EXAM_DAY) {
                counter.workingDays++;
                totalWorking++;
            } else {
                counter.holidays++;
                totalHoliday++;
            }

            cursor = cursor.plusDays(1);
        }

        List<MonthSummaryDTO> months = monthlyCounter.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> academicMonthOrder(entry.getKey())))
                .map(entry -> new MonthSummaryDTO(
                        entry.getKey(),
                        Month.of(entry.getKey()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        entry.getValue().workingDays,
                        entry.getValue().holidays,
                        entry.getValue().halfDays
                ))
                .toList();

        return new CalendarSummaryDTO(academicYear, totalWorking, totalHoliday, totalHalfDay, months);
    }

    private AcademicCalendarEvent findById(Long id) {
        AcademicCalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found with id: " + id));

        if (!event.isActive()) {
            throw new ResourceNotFoundException("Calendar event not found with id: " + id);
        }
        return event;
    }

    private AcademicCalendarEvent findByIdentifier(String identifier) {
        AcademicCalendarEvent event = PublicIdentifierResolver.resolve(
                identifier,
                calendarEventRepository::findByUuid,
                calendarEventRepository::findById,
                "Calendar event"
        );
        if (!event.isActive()) {
            throw new ResourceNotFoundException("Calendar event not found with identifier: " + identifier);
        }
        return event;
    }

    private void mapCreateOrUpdate(
            String academicYear,
            LocalDate date,
            DayType dayType,
            String title,
            String description,
            Boolean appliesToStaff,
            Boolean appliesToStudents,
            AcademicCalendarEvent event
    ) {
        event.setAcademicYear(academicYear);
        event.setDate(date);
        event.setDayType(dayType);
        event.setTitle(title);
        event.setDescription(description);
        event.setAppliesToStaff(appliesToStaff == null || appliesToStaff);
        event.setAppliesToStudents(appliesToStudents == null || appliesToStudents);
    }

    private CalendarEventResponseDTO toResponse(AcademicCalendarEvent event) {
        return new CalendarEventResponseDTO(
                event.getId(),
                event.getUuid() != null ? event.getUuid().toString() : null,
                event.getAcademicYear(),
                event.getDate(),
                event.getDayType(),
                event.getTitle(),
                event.getDescription(),
                event.isAppliesToStaff(),
                event.isAppliesToStudents(),
                event.getCreatedAt()
        );
    }

    private DayType defaultDayType(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) ? DayType.HOLIDAY : DayType.WORKING;
    }

    private int resolveCalendarYear(String academicYear, Integer month) {
        YearRange yearRange = parseAcademicYear(academicYear);
        return month >= Month.APRIL.getValue() ? yearRange.startYear() : yearRange.endYear();
    }

    private YearRange parseAcademicYear(String academicYear) {
        String[] tokens = academicYear.split("-");
        if (tokens.length != 2) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }

        try {
            int startYear = Integer.parseInt(tokens[0]);
            int endYear = Integer.parseInt(tokens[1]);
            if (endYear != startYear + 1) {
                throw new EdusyncException("Invalid academicYear range. Example: 2025-2026", HttpStatus.BAD_REQUEST);
            }
            return new YearRange(startYear, endYear);
        } catch (NumberFormatException ex) {
            throw new EdusyncException("Invalid academicYear format. Expected YYYY-YYYY", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateMonth(Integer month) {
        if (month < 1 || month > 12) {
            throw new EdusyncException("Month must be between 1 and 12", HttpStatus.BAD_REQUEST);
        }
    }

    private int academicMonthOrder(int month) {
        return month >= Month.APRIL.getValue() ? (month - Month.APRIL.getValue()) : (month + 9);
    }

    private record YearRange(int startYear, int endYear) {
    }

    private static class Counter {
        int workingDays;
        int holidays;
        int halfDays;
    }
}

