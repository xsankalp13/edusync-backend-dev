package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ExpenseClaim;
import com.project.edusync.hrms.model.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {
    Optional<ExpenseClaim> findByUuid(UUID uuid);
    List<ExpenseClaim> findByStaff_Id(Long staffId);
    List<ExpenseClaim> findByStaff_IdAndStatus(Long staffId, ExpenseStatus status);
    List<ExpenseClaim> findByStatus(ExpenseStatus status);
}

