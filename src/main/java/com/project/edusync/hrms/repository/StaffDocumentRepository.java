package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffDocumentRepository extends JpaRepository<StaffDocument, Long> {
    List<StaffDocument> findByStaff_IdAndActiveTrue(Long staffId);
    Optional<StaffDocument> findByUuidAndActiveTrue(UUID uuid);
    Optional<StaffDocument> findByStaff_IdAndUuidAndActiveTrue(Long staffId, UUID uuid);
}

