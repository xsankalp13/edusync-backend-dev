package com.project.edusync.uis.model.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StudentMessageDTO {
    private Long id;
    private Long senderUserId;
    private Long receiverUserId;
    private Long studentId;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;
}

