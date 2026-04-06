package com.project.edusync.hrms.exception;

import org.springframework.http.HttpStatus;

public class LeaveConflictException extends LeaveModuleException {
    public LeaveConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

