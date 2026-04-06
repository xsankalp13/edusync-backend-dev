package com.project.edusync.teacher.repository;

import com.project.edusync.teacher.model.entity.ProxyRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProxyRequestRepository extends JpaRepository<ProxyRequest, Long> {
    List<ProxyRequest> findByRequestedToId(Long teacherId);
}