package com.project.edusync.hrms.repository;

import com.project.edusync.hrms.model.entity.LeaveTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTemplateItemRepository extends JpaRepository<LeaveTemplateItem, Long> {
    Optional<LeaveTemplateItem> findByUuid(UUID uuid);
}
