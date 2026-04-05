package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.AcademicCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Long> {

    List<AcademicCalendarEvent> findByIsActiveTrueOrderByDateAsc();

    List<AcademicCalendarEvent> findByAcademicYearAndIsActiveTrueOrderByDateAsc(String academicYear);

    List<AcademicCalendarEvent> findByAcademicYearAndDateBetweenAndIsActiveTrueOrderByDateAsc(
            String academicYear,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<AcademicCalendarEvent> findByAcademicYearAndDateAndIsActiveTrue(String academicYear, LocalDate date);

    boolean existsByAcademicYearAndDateAndIsActiveTrue(String academicYear, LocalDate date);

    boolean existsByAcademicYearAndDateAndIsActiveTrueAndIdNot(String academicYear, LocalDate date, Long id);

    Optional<AcademicCalendarEvent> findByUuid(java.util.UUID uuid);
}

