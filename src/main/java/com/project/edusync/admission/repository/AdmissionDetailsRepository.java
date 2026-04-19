package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.AdmissionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AdmissionDetailsRepository extends JpaRepository<AdmissionDetails, Long> {
    AdmissionDetails findByApplication_Id(Long applicationId);
}
