package com.project.edusync.admission.repository;

import com.project.edusync.admission.model.entity.AdmissionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, Long> {
    Optional<AdmissionApplication> findByUser_Id(Long userId);
    Optional<AdmissionApplication> findByUuid(UUID uuid);
}
