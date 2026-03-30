package com.project.edusync.adm.repository;

import com.project.edusync.adm.model.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndUuidNot(String name, UUID uuid);

    Optional<Building> findByNameIgnoreCase(String name);
}

