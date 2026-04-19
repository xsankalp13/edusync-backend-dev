package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.AcademicInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AcademicInformationRepository extends JpaRepository<AcademicInformation, Long> {
    AcademicInformation findByApplication_Id(Long applicationId);
}
