package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGuardianRelationship extends JpaRepository<Guardian,Long> {
}
