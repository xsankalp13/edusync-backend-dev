package com.project.edusync.teacher.repository;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.teacher.model.entity.LectureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LectureLogRepository extends JpaRepository<LectureLog, Long> {
    Optional<LectureLog> findByScheduleUuidAndTeacherId(UUID scheduleUuid, Integer teacherId);
    Optional<LectureLog> findByScheduleAndTeacher(Schedule schedule, User teacher);
    /** Used by students to read a log without ownership check. */
    Optional<LectureLog> findByScheduleUuid(UUID scheduleUuid);
}
