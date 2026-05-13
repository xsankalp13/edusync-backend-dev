package com.project.edusync.uis.model.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentContactDTO {
    private Long userId;
    private String name;
    private String role;
    private int unreadCount;
}
