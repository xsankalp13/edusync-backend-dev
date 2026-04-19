package com.project.edusync.admission.repository;
import com.project.edusync.admission.model.entity.DeclarationSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DeclarationSectionRepository extends JpaRepository<DeclarationSection, Long> {
    DeclarationSection findByApplication_Id(Long applicationId);
}
