package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ExitClearanceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExitClearanceItemRepository extends JpaRepository<ExitClearanceItem, Long> {
    List<ExitClearanceItem> findByExitRequest_IdAndActiveTrue(Long exitRequestId);
    Optional<ExitClearanceItem> findByIdAndExitRequest_Id(Long id, Long exitRequestId);
}

