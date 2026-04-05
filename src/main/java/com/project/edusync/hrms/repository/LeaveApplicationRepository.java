package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LeaveApplication;
import com.project.edusync.hrms.model.enums.LeaveApplicationStatus;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Optional;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    @Query("""
            SELECT la FROM LeaveApplication la
            WHERE la.active = true
              AND (:staffId IS NULL OR la.staff.id = :staffId)
              AND (:status IS NULL OR la.status = :status)
              AND (:leaveTypeCode IS NULL OR UPPER(la.leaveType.leaveCode) = :leaveTypeCode)
              AND (:fromDate IS NULL OR la.toDate >= :fromDate)
              AND (:toDate IS NULL OR la.fromDate <= :toDate)
            """)
    Page<LeaveApplication> search(
            @Param("staffId") Long staffId,
            @Param("status") LeaveApplicationStatus status,
            @Param("leaveTypeCode") String leaveTypeCode,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(la) > 0 FROM LeaveApplication la
            WHERE la.active = true
              AND la.staff.id = :staffId
              AND la.status IN :statuses
              AND la.fromDate <= :toDate
              AND la.toDate >= :fromDate
            """)
    boolean existsOverlapping(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") java.util.Collection<LeaveApplicationStatus> statuses
    );

    @Query("""
            SELECT COUNT(la) > 0 FROM LeaveApplication la
            WHERE la.active = true
              AND la.id <> :applicationId
              AND la.staff.id = :staffId
              AND la.status IN :statuses
              AND la.fromDate <= :toDate
              AND la.toDate >= :fromDate
            """)
    boolean existsOverlappingExcludingId(
            @Param("applicationId") Long applicationId,
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") java.util.Collection<LeaveApplicationStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(la.totalDays), 0)
            FROM LeaveApplication la
            WHERE la.active = true
              AND la.staff.id = :staffId
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND UPPER(la.leaveType.leaveCode) = 'LOP'
              AND la.fromDate <= :toDate
              AND la.toDate >= :fromDate
            """)
    BigDecimal sumApprovedLopDaysOverlapping(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    long countByActiveTrueAndStatus(LeaveApplicationStatus status);

    @Query("""
            SELECT COUNT(DISTINCT la.staff.id)
            FROM LeaveApplication la
            WHERE la.active = true
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND la.fromDate <= :date
              AND la.toDate >= :date
            """)
    long countDistinctStaffOnApprovedLeave(@Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(DISTINCT la.staff.id)
            FROM LeaveApplication la
            WHERE la.active = true
              AND la.staff.isActive = true
              AND la.staff.category = :category
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND la.fromDate <= :date
              AND la.toDate >= :date
            """)
    long countDistinctStaffOnApprovedLeaveByCategoryAndDate(@Param("category") StaffCategory category, @Param("date") LocalDate date);

    Optional<LeaveApplication> findByUuid(java.util.UUID uuid);
}
