package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.PayslipLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayslipLineItemRepository extends JpaRepository<PayslipLineItem, Long> {

    List<PayslipLineItem> findByPayslip_IdAndActiveTrueOrderByIdAsc(Long payslipId);
}

