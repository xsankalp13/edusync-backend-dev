package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Guardian;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.StudentGuardianRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentGuardianRelationshipRepository extends JpaRepository<StudentGuardianRelationship, Long> {
    Optional<StudentGuardianRelationship> findByStudentAndGuardian(Student student, Guardian guardian);
    boolean existsByStudentAndGuardian(Student student, Guardian guardian);
    List<StudentGuardianRelationship> findByStudent(Student student);
    List<StudentGuardianRelationship> findByGuardian(Guardian guardian);

    @Query("""
            SELECT sgr
            FROM StudentGuardianRelationship sgr
            JOIN FETCH sgr.guardian g
            JOIN FETCH sgr.student s
            JOIN FETCH s.userProfile sup
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE g.id IN :guardianIds
            """)
    List<StudentGuardianRelationship> findAllWithStudentGraphByGuardianIds(@Param("guardianIds") List<Long> guardianIds);

    @Query("""
            SELECT sgr
            FROM StudentGuardianRelationship sgr
            JOIN FETCH sgr.guardian g
            JOIN FETCH g.userProfile gp
            WHERE sgr.student.id IN :studentIds
              AND sgr.isPrimaryContact = true
            """)
    List<StudentGuardianRelationship> findPrimaryContactsByStudentIds(@Param("studentIds") List<Long> studentIds);

    void deleteByStudentAndGuardian(Student student, Guardian guardian);
}

