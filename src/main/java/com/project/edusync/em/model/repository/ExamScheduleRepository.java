package com.project.edusync.em.model.repository;

import com.project.edusync.em.model.entity.ExamSchedule;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.model.entity.Timeslot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule , Long> {
List<ExamSchedule> findByExamUuid (UUID examUuid);

@Query("""
		SELECT es FROM ExamSchedule es
		JOIN FETCH es.subject sub
		JOIN FETCH es.exam ex
		WHERE es.academicClass.id = :sectionId
		  AND es.examDate >= :fromDate
		ORDER BY es.examDate ASC
		""")
List<ExamSchedule> findUpcomingForSection(@Param("sectionId") Long sectionId,
										  @Param("fromDate") LocalDate fromDate,
										  Pageable pageable);

    boolean existsByAcademicClassAndTimeslotAndExamDate(AcademicClass academicClass, Timeslot timeslot, java.time.LocalDate examDate);
    boolean existsByAcademicClassAndSubjectAndExamDate(AcademicClass academicClass, Subject subject, java.time.LocalDate examDate);
    java.util.List<ExamSchedule> findByAcademicClass(AcademicClass academicClass);
}
