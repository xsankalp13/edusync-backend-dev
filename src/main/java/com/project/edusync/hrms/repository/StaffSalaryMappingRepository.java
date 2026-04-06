package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffSalaryMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffSalaryMappingRepository extends JpaRepository<StaffSalaryMapping, Long> {

    Page<StaffSalaryMapping> findByActiveTrue(Pageable pageable);

    List<StaffSalaryMapping> findByStaff_IdAndActiveTrueOrderByEffectiveFromDesc(Long staffId);

    Optional<StaffSalaryMapping> findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
            Long staffId,
            LocalDate date
    );

    Optional<StaffSalaryMapping> findFirstByStaff_IdAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
            Long staffId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT COUNT(m) > 0 FROM StaffSalaryMapping m
            WHERE m.active = true
              AND m.staff.id = :staffId
              AND (:excludeMappingId IS NULL OR m.id <> :excludeMappingId)
              AND m.effectiveFrom <= :newTo
              AND COALESCE(m.effectiveTo, :maxDate) >= :newFrom
            """)
    boolean existsOverlappingRange(
            @Param("staffId") Long staffId,
            @Param("newFrom") LocalDate newFrom,
            @Param("newTo") LocalDate newTo,
            @Param("maxDate") LocalDate maxDate,
            @Param("excludeMappingId") Long excludeMappingId
    );

    @Query("""
            SELECT m FROM StaffSalaryMapping m
            WHERE m.active = true
              AND m.effectiveFrom <= :rangeEnd
              AND COALESCE(m.effectiveTo, :maxDate) >= :rangeStart
            ORDER BY m.staff.id ASC, m.effectiveFrom DESC
            """)
    List<StaffSalaryMapping> findActiveMappingsEffectiveInRange(
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd,
            @Param("maxDate") LocalDate maxDate
    );

    @Query("""
            SELECT COUNT(DISTINCT m.staff.id)
            FROM StaffSalaryMapping m
            WHERE m.active = true
              AND m.effectiveFrom <= :onDate
              AND COALESCE(m.effectiveTo, :maxDate) >= :onDate
            """)
    long countDistinctStaffWithActiveMappingOnDate(
            @Param("onDate") LocalDate onDate,
            @Param("maxDate") LocalDate maxDate
    );

    Optional<StaffSalaryMapping> findByUuid(java.util.UUID uuid);
}


