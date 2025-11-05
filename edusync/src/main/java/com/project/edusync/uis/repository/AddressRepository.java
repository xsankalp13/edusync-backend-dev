package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
