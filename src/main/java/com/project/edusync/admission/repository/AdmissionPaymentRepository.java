package com.project.edusync.admission.repository;

import com.project.edusync.admission.model.entity.AdmissionApplication;
import com.project.edusync.admission.model.entity.AdmissionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionPaymentRepository extends JpaRepository<AdmissionPayment, Long> {
    Optional<AdmissionPayment> findByApplication_Id(Long applicationId);
    Optional<AdmissionPayment> findByRazorpayOrderId(String orderId);
    List<AdmissionPayment> findByApplication(AdmissionApplication application);
}
