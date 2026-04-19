package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ExitRequest;
import com.project.edusync.hrms.model.enums.ExitRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExitRequestRepository extends JpaRepository<ExitRequest, Long> {
    Optional<ExitRequest> findByUuid(UUID uuid);
    Optional<ExitRequest> findByUuidAndActiveTrue(UUID uuid);
    List<ExitRequest> findByStatus(ExitRequestStatus status);
    List<ExitRequest> findAllByActiveTrue();
    List<ExitRequest> findByStaff_Id(Long staffId);
}

