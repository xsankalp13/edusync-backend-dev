package com.project.edusync.teacher.service;

import com.project.edusync.teacher.model.entity.LessonPlan;
import com.project.edusync.teacher.repository.LessonPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonPlanService {

    private final LessonPlanRepository lessonPlanRepository;

    public List<LessonPlan> getLessonPlans(Long teacherId) {
        return lessonPlanRepository.findByTeacherId(teacherId);
    }

    public LessonPlan saveLessonPlan(LessonPlan lessonPlan) {
        return lessonPlanRepository.save(lessonPlan);
    }
}