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
interface AdmitCardScheduleProjection {
	Long getId();
	Long getAcademicClassId();
	String getAcademicClassName();
	Long getSectionId();
	String getSectionName();
	Long getSubjectId();
	String getSubjectName();
	LocalDate getExamDate();
	java.time.LocalTime getStartTime();
	java.time.LocalTime getEndTime();
	Integer getActiveStudentCount();
}

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

	@Query("""
			SELECT es FROM ExamSchedule es
			JOIN FETCH es.exam ex
			JOIN FETCH es.subject sub
			JOIN FETCH es.timeslot ts
			JOIN FETCH es.academicClass ac
			LEFT JOIN FETCH es.section sec
			WHERE ex.id = :examId
			ORDER BY es.examDate ASC, ts.startTime ASC
			""")
	java.util.List<ExamSchedule> findByExamIdWithDetails(@Param("examId") Long examId);

	@Query("""
			SELECT es.id AS id,
			       ac.id AS academicClassId,
			       ac.name AS academicClassName,
			       sec.id AS sectionId,
			       sec.sectionName AS sectionName,
			       sub.id AS subjectId,
			       sub.name AS subjectName,
			       es.examDate AS examDate,
			       ts.startTime AS startTime,
			       ts.endTime AS endTime,
			       es.activeStudentCount AS activeStudentCount
			FROM ExamSchedule es
			JOIN es.academicClass ac
			JOIN es.subject sub
			JOIN es.timeslot ts
			LEFT JOIN es.section sec
			WHERE es.exam.id = :examId
			ORDER BY es.examDate ASC, ts.startTime ASC
			""")
	List<AdmitCardScheduleProjection> findAdmitCardSchedulesByExamId(@Param("examId") Long examId);

	@Query("""
			SELECT es.id, COUNT(DISTINCT st.id)
			FROM ExamSchedule es
			JOIN com.project.edusync.uis.model.entity.Student st
			  ON st.isActive = true
			 AND (
			      (es.section IS NOT NULL AND st.section.id = es.section.id)
			      OR
			      (es.section IS NULL AND st.section.academicClass.id = es.academicClass.id)
			 )
			WHERE es.exam.id = :examId
			GROUP BY es.id
			""")
	java.util.List<Object[]> countActiveStudentsPerSchedule(@Param("examId") Long examId);
}
