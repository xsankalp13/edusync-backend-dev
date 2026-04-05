package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.StaffDesignation;
import com.project.edusync.uis.model.enums.StaffCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffDesignationRepository extends JpaRepository<StaffDesignation, Long> {

    List<StaffDesignation> findByActiveTrueOrderBySortOrderAscDesignationNameAsc();

    List<StaffDesignation> findByCategoryAndActiveOrderBySortOrderAscDesignationNameAsc(StaffCategory category, boolean active);

    List<StaffDesignation> findByCategoryOrderBySortOrderAscDesignationNameAsc(StaffCategory category);

    List<StaffDesignation> findByActiveOrderBySortOrderAscDesignationNameAsc(boolean active);

    Optional<StaffDesignation> findByDesignationCodeIgnoreCaseAndActiveTrue(String designationCode);

    boolean existsByDesignationCodeIgnoreCaseAndActiveTrue(String designationCode);

    boolean existsByDesignationCodeIgnoreCaseAndActiveTrueAndIdNot(String designationCode, Long id);

    Optional<StaffDesignation> findByUuid(java.util.UUID uuid);
}


