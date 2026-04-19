package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.PickupRequest;
import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {
    Optional<PickupRequest> findByQrToken(String qrToken);
    List<PickupRequest> findByStudentOrderByCreatedAtDesc(Student student);
    List<PickupRequest> findByGeneratedBy_IdOrderByCreatedAtDesc(Long generatedById);
}
