package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.SelfAppraisalReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SelfAppraisalReviewRepository extends JpaRepository<SelfAppraisalReview, Long> {
    Optional<SelfAppraisalReview> findByCycle_IdAndStaff_Id(Long cycleId, Long staffId);
    List<SelfAppraisalReview> findByCycle_Id(Long cycleId);
}

