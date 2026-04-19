package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.CompOffRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompOffRecordRepository extends JpaRepository<CompOffRecord, Long> {
    Optional<CompOffRecord> findByUuid(UUID uuid);
    List<CompOffRecord> findByStaff_Id(Long staffId);
    List<CompOffRecord> findByStaff_IdAndCredited(Long staffId, boolean credited);
}

