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
import java.util.List;
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

    @Query("""
            SELECT la FROM LeaveApplication la
            WHERE la.active = true
              AND la.staff.id = :staffId
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND la.fromDate <= :toDate
              AND la.toDate >= :fromDate
            """)
    List<LeaveApplication> findApprovedActiveByStaffIdAndDateRange(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    Optional<LeaveApplication> findByUuid(java.util.UUID uuid);

    interface LeaveCountByDateProjection {
        java.time.LocalDate getLeaveDate();
        Long getOnLeaveCount();
    }

    /**
     * Returns all approved leave applications that overlap with the given date range.
     * The service layer aggregates per-day on-leave counts in-memory from the result.
     * This is 1 query replacing N individual countDistinctStaffOnApprovedLeave calls.
     * Returns (fromDate, toDate, staffId) rows for the service to expand.
     */
    @Query("""
            SELECT la FROM LeaveApplication la
            WHERE la.active = true
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND la.fromDate <= :endDate
              AND la.toDate >= :startDate
            """)
    java.util.List<LeaveApplication> findApprovedLeaveOverlappingRange(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    interface CategoryLeaveCountProjection {
        com.project.edusync.uis.model.enums.StaffCategory getCategory();
        Long getOnLeaveCount();
    }

    /**
     * Returns (category, onLeaveCount) for active staff on approved leave on specific date.
     * Replaces 3 individual per-category leave count queries in buildCategoryAttendance().
     */
    @Query("""
            SELECT la.staff.category as category,
                   COUNT(DISTINCT la.staff.id) as onLeaveCount
            FROM LeaveApplication la
            WHERE la.active = true
              AND la.staff.isActive = true
              AND la.status = com.project.edusync.hrms.model.enums.LeaveApplicationStatus.APPROVED
              AND la.fromDate <= :date
              AND la.toDate >= :date
            GROUP BY la.staff.category
            """)
    List<CategoryLeaveCountProjection> countOnLeaveByDateGroupedByCategory(
            @Param("date") java.time.LocalDate date
    );
}
