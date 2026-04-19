package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.MedicalInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface MedicalInformationRepository extends JpaRepository<MedicalInformation, Long> {
    MedicalInformation findByApplication_Id(Long applicationId);
}
