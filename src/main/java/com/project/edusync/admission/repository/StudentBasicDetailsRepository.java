package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.StudentBasicDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface StudentBasicDetailsRepository extends JpaRepository<StudentBasicDetails, Long> {
    StudentBasicDetails findByApplication_Id(Long applicationId);
}
