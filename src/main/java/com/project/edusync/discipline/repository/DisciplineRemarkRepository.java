package com.project.edusync.discipline.repository;

import com.project.edusync.discipline.model.entity.DisciplineRemark;
import com.project.edusync.discipline.model.enums.RemarkTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisciplineRemarkRepository extends JpaRepository<DisciplineRemark, Long>, JpaSpecificationExecutor<DisciplineRemark> {

    /**
     * Get all remarks for a specific student (for student dashboard).
     */
    @Query("""
            SELECT r FROM DisciplineRemark r
            JOIN FETCH r.teacher t
            JOIN FETCH t.userProfile tp
            WHERE r.student.id = :studentId
            ORDER BY r.remarkDate DESC, r.createdAt DESC
            """)
    List<DisciplineRemark> findByStudentIdWithTeacher(@Param("studentId") Long studentId);

    /**
     * Get all remarks created by a specific teacher.
     */
    @Query("""
            SELECT r FROM DisciplineRemark r
            JOIN FETCH r.student s
            JOIN FETCH s.userProfile sp
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE r.teacher.id = :teacherId
            ORDER BY r.remarkDate DESC, r.createdAt DESC
            """)
    List<DisciplineRemark> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    long countByTag(RemarkTag tag);

    /**
     * Paginated admin query with full details, used with Specification executor for dynamic filtering.
     */
    @Query(value = """
            SELECT r FROM DisciplineRemark r
            JOIN FETCH r.student s
            JOIN FETCH s.userProfile sp
            JOIN FETCH r.teacher t
            JOIN FETCH t.userProfile tp
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            """,
            countQuery = """
            SELECT COUNT(r) FROM DisciplineRemark r
            JOIN r.student s
            JOIN s.section sec
            JOIN sec.academicClass ac
            """)
    Page<DisciplineRemark> findAllWithFullDetails(Pageable pageable);
}
