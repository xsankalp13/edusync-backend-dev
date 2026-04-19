package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.CourseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCertificateRepository extends JpaRepository<CourseCertificate, Long> {
    Optional<CourseCertificate> findByEnrollment_Id(Long enrollmentId);
}

