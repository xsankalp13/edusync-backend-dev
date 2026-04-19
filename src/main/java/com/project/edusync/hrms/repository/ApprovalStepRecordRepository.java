package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.ApprovalStepRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalStepRecordRepository extends JpaRepository<ApprovalStepRecord, Long> {
    List<ApprovalStepRecord> findByRequest_IdOrderByStepOrderAsc(Long requestId);
}

