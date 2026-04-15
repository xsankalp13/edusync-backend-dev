package com.project.edusync.admission.repository;

import com.project.edusync.admission.model.entity.AdmissionEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdmissionEnquiryRepository extends JpaRepository<AdmissionEnquiry, Long> {
    List<AdmissionEnquiry> findByUser_Id(Long userId);
    List<AdmissionEnquiry> findByUuid(UUID uuid);
}
