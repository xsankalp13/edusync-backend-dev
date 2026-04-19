package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.PromotionRequest;
import com.project.edusync.hrms.model.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRequestRepository extends JpaRepository<PromotionRequest, Long> {
    Optional<PromotionRequest> findByUuid(UUID uuid);
    Page<PromotionRequest> findByStatusAndActiveTrue(PromotionStatus status, Pageable pageable);
    Page<PromotionRequest> findByActiveTrue(Pageable pageable);
    List<PromotionRequest> findByStaff_IdAndActiveTrueOrderByEffectiveDateDesc(Long staffId);
}
