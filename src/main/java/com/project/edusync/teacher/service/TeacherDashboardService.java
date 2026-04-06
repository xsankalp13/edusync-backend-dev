package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.dto.TeacherDashboardSummaryResponseDto;
import com.project.edusync.teacher.model.dto.TeacherHomeroomResponseDto;
import com.project.edusync.teacher.model.dto.TeacherMyClassesResponseDto;
import com.project.edusync.teacher.model.dto.TeacherScheduleResponseDto;
import com.project.edusync.teacher.model.dto.TeacherStudentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TeacherDashboardService {
    List<TeacherMyClassesResponseDto> getMyClasses(Long currentUserId);

    List<TeacherMyClassesResponseDto> getMyClassTeacherSections(Long currentUserId);

    Page<TeacherStudentResponseDto> getMyStudents(Long currentUserId,
                                                  UUID classUuid,
                                                  UUID sectionUuid,
                                                  String search,
                                                  Pageable pageable);

    Page<TeacherStudentResponseDto> getClassTeacherStudents(Long currentUserId,
                                                            UUID sectionUuid,
                                                            String search,
                                                            Pageable pageable);

    TeacherScheduleResponseDto getMySchedule(Long currentUserId, LocalDate date);

    TeacherDashboardSummaryResponseDto getDashboardSummary(Long currentUserId, LocalDate date);

    TeacherHomeroomResponseDto getMyHomeroom(Long currentUserId, LocalDate date);
}