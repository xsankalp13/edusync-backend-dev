package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ManagerAppraisalReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManagerAppraisalReviewRepository extends JpaRepository<ManagerAppraisalReview, Long> {
    Optional<ManagerAppraisalReview> findByCycle_IdAndStaff_Id(Long cycleId, Long staffId);
    List<ManagerAppraisalReview> findByCycle_Id(Long cycleId);
}

