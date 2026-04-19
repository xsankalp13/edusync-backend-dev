package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ApprovalChainStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalChainStepRepository extends JpaRepository<ApprovalChainStep, Long> {
    List<ApprovalChainStep> findByChainConfig_IdOrderByStepOrderAsc(Long chainConfigId);
    void deleteByChainConfig_Id(Long chainConfigId);
}

