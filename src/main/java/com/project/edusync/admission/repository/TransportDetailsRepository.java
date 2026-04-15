package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.TransportDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface TransportDetailsRepository extends JpaRepository<TransportDetails, Long> {
    TransportDetails findByApplication_Id(Long applicationId);
}
