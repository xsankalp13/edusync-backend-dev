package com.project.edusync.em.model.service.serviceImpl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.edusync.em.model.repository.ExamScheduleRepository;
import com.project.edusync.em.model.repository.SeatAllocationRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdmitCardGenerationReadCacheService {

    private final StudentRepository studentRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final ExamScheduleRepository examScheduleRepository;

    private final Cache<String, List<StudentRepository.AdmitCardStudentProjection>> studentListCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(500)
            .build();

    private final Cache<Long, List<SeatAllocationRepository.AdmitCardSeatAllocationProjection>> seatAllocationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(500)
            .build();

    private final Cache<Long, List<ExamScheduleRepository.AdmitCardScheduleProjection>> examScheduleCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(200)
            .build();

    private final Cache<Long, Integer> scheduleStudentCountCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1000)
            .build();

    public List<StudentRepository.AdmitCardStudentProjection> getActiveStudentsBySectionId(Long sectionId) {
        String key = "section:" + sectionId;
        return studentListCache.get(key,
                ignored -> List.copyOf(studentRepository.findActiveAdmitCardStudentsBySectionIds(List.of(sectionId))));
    }

    public List<StudentRepository.AdmitCardStudentProjection> getActiveStudentsByClassId(Long classId) {
        String key = "class:" + classId;
        return studentListCache.get(key,
                ignored -> List.copyOf(studentRepository.findActiveAdmitCardStudentsByClassIds(List.of(classId))));
    }

    public List<SeatAllocationRepository.AdmitCardSeatAllocationProjection> getSeatAllocationsForSchedule(Long scheduleId) {
        return seatAllocationCache.get(scheduleId,
                ignored -> List.copyOf(seatAllocationRepository.findAdmitCardAllocationsByScheduleId(scheduleId)));
    }

    public List<ExamScheduleRepository.AdmitCardScheduleProjection> getAdmitCardSchedules(Long examId) {
        return examScheduleCache.get(examId,
                ignored -> List.copyOf(examScheduleRepository.findAdmitCardSchedulesByExamId(examId)));
    }

    public int getCachedActiveStudentCountForSchedule(ExamScheduleRepository.AdmitCardScheduleProjection schedule) {
        return scheduleStudentCountCache.get(schedule.getId(),
                ignored -> schedule.getActiveStudentCount() == null ? 0 : Math.max(schedule.getActiveStudentCount(), 0));
    }
}

