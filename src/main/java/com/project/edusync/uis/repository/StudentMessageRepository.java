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
    int countByStudent_IdAndReceiver_IdAndSender_IdAndReadFalse(Long studentId, Long receiverId, Long senderId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE com.project.edusync.uis.model.entity.messaging.StudentMessage m SET m.read = true WHERE m.student.id = :studentId AND m.receiver.id = :userId AND m.sender.id = :otherUserId AND m.read = false")
    void markAsRead(@Param("studentId") Long studentId, @Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Query("SELECT m FROM com.project.edusync.uis.model.entity.messaging.StudentMessage m WHERE m.receiver.id = :receiverId AND m.read = false ORDER BY m.sentAt DESC")
    List<StudentMessage> findUnreadByReceiver(@Param("receiverId") Long receiverId);
}

