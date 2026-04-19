package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ExpenseClaimItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseClaimItemRepository extends JpaRepository<ExpenseClaimItem, Long> {
    List<ExpenseClaimItem> findByClaim_IdAndActiveTrue(Long claimId);
    Optional<ExpenseClaimItem> findByIdAndClaim_Id(Long id, Long claimId);
}

