package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress,Long> {
}
