package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffSalaryComponentOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffSalaryComponentOverrideRepository extends JpaRepository<StaffSalaryComponentOverride, Long> {

    List<StaffSalaryComponentOverride> findByMapping_IdAndActiveTrue(Long mappingId);

    void deleteByMapping_Id(Long mappingId);
}

