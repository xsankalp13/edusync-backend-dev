package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, Long> {

    interface AdmitCardStudentProjection {
        Long getId();
        String getEnrollmentNumber();
        Integer getRollNo();
        String getFirstName();
        String getLastName();
        Long getSectionId();
        Long getClassId();
    }

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    Optional<Student> findByUserProfile(UserProfile profile);

    /**
     * Fetches all students with their UserProfile, User, Section and AcademicClass eagerly.
     * Explicit countQuery is required when using JOIN FETCH with pagination.
     */
    @Query(value = "SELECT s FROM Student s " +
                   "JOIN FETCH s.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "JOIN FETCH s.section sec " +
                   "JOIN FETCH sec.academicClass ac " +
                   "WHERE (:active IS NULL OR u.isActive = :active) " +
                   "AND (:classUuid IS NULL OR CAST(ac.uuid AS string) = :classUuid)",
           countQuery = "SELECT COUNT(s) FROM Student s " +
                        "JOIN s.userProfile up " +
                        "JOIN up.user u " +
                        "JOIN s.section sec " +
                        "JOIN sec.academicClass ac " +
                        "WHERE (:active IS NULL OR u.isActive = :active) " +
                        "AND (:classUuid IS NULL OR CAST(ac.uuid AS string) = :classUuid)")
    Page<Student> findAllWithDetails(@Param("active") Boolean active, @Param("classUuid") String classUuid, Pageable pageable);

    /**
     * Search students by name, email, or enrollment number (case-insensitive).
     * Explicit countQuery required for JOIN FETCH + pagination.
     */
    @Query(value = "SELECT s FROM Student s " +
                   "JOIN FETCH s.userProfile up " +
                   "JOIN FETCH up.user u " +
                   "JOIN FETCH s.section sec " +
                   "JOIN FETCH sec.academicClass ac " +
                   "WHERE (:active IS NULL OR u.isActive = :active) " +
                   "AND (:classUuid IS NULL OR CAST(ac.uuid AS string) = :classUuid) " +
                   "AND (" +
                   "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(up.lastName)     LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "OR LOWER(s.enrollmentNumber) LIKE LOWER(CONCAT('%', :query, '%'))) ",
           countQuery = "SELECT COUNT(s) FROM Student s " +
                        "JOIN s.userProfile up " +
                        "JOIN up.user u " +
                        "JOIN s.section sec " +
                        "JOIN sec.academicClass ac " +
                        "WHERE (:active IS NULL OR u.isActive = :active) " +
                        "AND (:classUuid IS NULL OR CAST(ac.uuid AS string) = :classUuid) " +
                        "AND (" +
                        "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(up.lastName)     LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(s.enrollmentNumber) LIKE LOWER(CONCAT('%', :query, '%'))) ")
    Page<Student> searchStudents(@Param("query") String query, @Param("active") Boolean active, @Param("classUuid") String classUuid, Pageable pageable);

    Optional<Student> findByUuid(java.util.UUID uuid);

    Optional<Student> findByUserProfile_User_Id(Long userId);

    long countByIsActiveTrue();

    Optional<Student> findByRollNo(Integer rollNo);

    List<Student> findBySectionId(Long sectionId);

    List<Student> findBySectionIdOrderByRollNoAsc(Long sectionId);

    Page<Student> findBySectionIdOrderByRollNoAsc(Long sectionId, Pageable pageable);

    List<Student> findBySection_AcademicClass_Id(Long classId);

    List<Student> findBySection_AcademicClass_IdOrderByRollNoAsc(Long classId);

    Page<Student> findBySection_AcademicClass_IdOrderByRollNoAsc(Long classId, Pageable pageable);

    long countBySectionId(Long sectionId);

    long countBySection_AcademicClass_Id(Long classId);

    long countBySection_AcademicClass_IdAndIsActiveTrue(Long classId);

    @Query("""
            SELECT s.section.academicClass.name, COUNT(s)
            FROM Student s
            WHERE s.isActive = true
            GROUP BY s.section.academicClass.name
            """)
    List<Object[]> countActiveStudentsGroupedByClassName();

    @Query("SELECT s FROM Student s WHERE s.section.academicClass.id = :classId")
    List<Student> findByAcademicClassId(@Param("classId") Long classId);

    @Query(value = """
            SELECT s FROM Student s
            JOIN FETCH s.userProfile up
            JOIN s.section sec
            JOIN sec.academicClass ac
            WHERE s.isActive = true
              AND sec.id IN :sectionIds
              AND (:classUuid IS NULL OR ac.uuid = :classUuid)
              AND (:sectionUuid IS NULL OR sec.uuid = :sectionUuid)
              AND (:searchEnabled = false
                   OR LOWER(CONCAT(COALESCE(up.firstName, ''), ' ', COALESCE(up.lastName, ''))) LIKE :searchPattern
                   OR LOWER(COALESCE(s.enrollmentNumber, '')) LIKE :searchPattern
                   OR STR(s.rollNo) LIKE :searchPattern)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Student s
            JOIN s.userProfile up
            JOIN s.section sec
            JOIN sec.academicClass ac
            WHERE s.isActive = true
              AND sec.id IN :sectionIds
              AND (:classUuid IS NULL OR ac.uuid = :classUuid)
              AND (:sectionUuid IS NULL OR sec.uuid = :sectionUuid)
              AND (:searchEnabled = false
                   OR LOWER(CONCAT(COALESCE(up.firstName, ''), ' ', COALESCE(up.lastName, ''))) LIKE :searchPattern
                   OR LOWER(COALESCE(s.enrollmentNumber, '')) LIKE :searchPattern
                   OR STR(s.rollNo) LIKE :searchPattern)
            """)
    Page<Student> findTeacherStudents(@Param("sectionIds") List<Long> sectionIds,
                                      @Param("classUuid") UUID classUuid,
                                      @Param("sectionUuid") UUID sectionUuid,
                                      @Param("searchEnabled") boolean searchEnabled,
                                      @Param("searchPattern") String searchPattern,
                                      Pageable pageable);

    long countBySection_IdAndIsActiveTrue(Long sectionId);

    @Query("SELECT s FROM Student s " +
           "JOIN FETCH s.userProfile up " +
           "JOIN FETCH up.user u " +
           "JOIN FETCH s.section sec " +
           "JOIN FETCH sec.academicClass ac " +
           "WHERE s.section.id = :sectionId AND s.isActive = true")
    java.util.List<Student> findAllBySectionIdWithDetails(@Param("sectionId") Long sectionId);

    @Query("SELECT s FROM Student s " +
           "JOIN FETCH s.userProfile up " +
           "JOIN FETCH up.user u " +
           "JOIN FETCH s.section sec " +
           "JOIN FETCH sec.academicClass ac " +
           "WHERE sec.uuid = :sectionUuid AND s.isActive = true")
    java.util.List<Student> findAllBySectionUuidWithDetails(@Param("sectionUuid") java.util.UUID sectionUuid);

    @Query("""
            SELECT s FROM Student s
            JOIN FETCH s.userProfile up
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE s.isActive = true
              AND sec.id IN :sectionIds
            ORDER BY sec.id ASC, s.rollNo ASC
            """)
    List<Student> findActiveBySectionIdsWithProfile(@Param("sectionIds") List<Long> sectionIds);

    @Query("""
            SELECT s FROM Student s
            JOIN FETCH s.userProfile up
            JOIN FETCH s.section sec
            JOIN FETCH sec.academicClass ac
            WHERE s.isActive = true
              AND ac.id IN :classIds
            ORDER BY ac.id ASC, sec.id ASC, s.rollNo ASC
            """)
    List<Student> findActiveByClassIdsWithProfile(@Param("classIds") List<Long> classIds);

    @Query("""
            SELECT s.id AS id,
                   s.enrollmentNumber AS enrollmentNumber,
                   s.rollNo AS rollNo,
                   up.firstName AS firstName,
                   up.lastName AS lastName,
                   sec.id AS sectionId,
                   sec.academicClass.id AS classId
            FROM Student s
            JOIN s.userProfile up
            JOIN s.section sec
            WHERE s.isActive = true
              AND sec.id IN :sectionIds
            ORDER BY sec.id ASC, s.rollNo ASC
            """)
    List<AdmitCardStudentProjection> findActiveAdmitCardStudentsBySectionIds(@Param("sectionIds") List<Long> sectionIds);

    @Query("""
            SELECT s.id AS id,
                   s.enrollmentNumber AS enrollmentNumber,
                   s.rollNo AS rollNo,
                   up.firstName AS firstName,
                   up.lastName AS lastName,
                   sec.id AS sectionId,
                   ac.id AS classId
            FROM Student s
            JOIN s.userProfile up
            JOIN s.section sec
            JOIN sec.academicClass ac
            WHERE s.isActive = true
              AND ac.id IN :classIds
            ORDER BY ac.id ASC, sec.id ASC, s.rollNo ASC
            """)
    List<AdmitCardStudentProjection> findActiveAdmitCardStudentsByClassIds(@Param("classIds") List<Long> classIds);
}
