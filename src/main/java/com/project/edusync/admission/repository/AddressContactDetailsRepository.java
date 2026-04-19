package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.AddressContactDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AddressContactDetailsRepository extends JpaRepository<AddressContactDetails, Long> {
    AddressContactDetails findByApplication_Id(Long applicationId);
}
