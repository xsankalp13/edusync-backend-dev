package com.project.edusync.hrms.exception;

import org.springframework.http.HttpStatus;

public class LeaveAccessDeniedException extends LeaveModuleException {
    public LeaveAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

