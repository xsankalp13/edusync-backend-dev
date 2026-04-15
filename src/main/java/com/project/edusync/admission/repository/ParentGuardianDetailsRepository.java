package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.ParentGuardianDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ParentGuardianDetailsRepository extends JpaRepository<ParentGuardianDetails, Long> {
    ParentGuardianDetails findByApplication_Id(Long applicationId);
}
