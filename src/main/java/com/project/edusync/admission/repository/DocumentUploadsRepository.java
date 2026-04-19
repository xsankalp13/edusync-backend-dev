package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.DocumentUploads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DocumentUploadsRepository extends JpaRepository<DocumentUploads, Long> {
    DocumentUploads findByApplication_Id(Long applicationId);
}
