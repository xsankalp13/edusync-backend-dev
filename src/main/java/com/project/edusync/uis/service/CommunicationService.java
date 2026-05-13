package com.project.edusync.uis.service;

import com.project.edusync.uis.model.dto.messaging.StudentMessageDTO;
import com.project.edusync.uis.model.dto.messaging.StudentMessageRequestDTO;

import java.util.List;

public interface CommunicationService {
    StudentMessageDTO sendMessage(Long senderUserId, Long studentId, StudentMessageRequestDTO request);

    List<StudentMessageDTO> getConversation(Long userId, Long studentId, Long otherUserId);
    void markConversationAsRead(Long userId, Long studentId, Long otherUserId);
}

