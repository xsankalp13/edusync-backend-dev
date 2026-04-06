package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LeaveBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByStaff_IdAndLeaveType_IdAndAcademicYearAndActiveTrue(Long staffId, Long leaveTypeId, String academicYear);

    List<LeaveBalance> findByStaff_IdAndAcademicYearAndActiveTrueOrderByLeaveType_LeaveCodeAsc(Long staffId, String academicYear);

    Page<LeaveBalance> findByAcademicYearAndActiveTrue(String academicYear, Pageable pageable);
}

