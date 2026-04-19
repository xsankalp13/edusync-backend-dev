package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.AppraisalGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppraisalGoalRepository extends JpaRepository<AppraisalGoal, Long> {
    List<AppraisalGoal> findByCycle_Id(Long cycleId);
    List<AppraisalGoal> findByCycle_IdAndStaff_Id(Long cycleId, Long staffId);
}

