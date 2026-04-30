package com.project.edusync.uis.repository;

import com.project.edusync.uis.model.entity.messaging.StudentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentMessageRepository extends JpaRepository<StudentMessage, Long> {

    @Query("SELECT m FROM com.project.edusync.uis.model.entity.messaging.StudentMessage m " +
            "WHERE m.student.id = :studentId AND ((m.sender.id = :a AND m.receiver.id = :b) OR (m.sender.id = :b AND m.receiver.id = :a)) " +
            "ORDER BY m.sentAt ASC")
    List<StudentMessage> findConversation(@Param("studentId") Long studentId, @Param("a") Long a, @Param("b") Long b);

    List<StudentMessage> findByStudent_Id(Long studentId);
}

