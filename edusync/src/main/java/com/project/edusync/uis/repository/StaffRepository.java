package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff,Long> {
    boolean existsByEmployeeId(String employeeId);
}
