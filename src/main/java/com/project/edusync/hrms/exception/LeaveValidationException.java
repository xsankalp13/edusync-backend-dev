package com.project.edusync.hrms.exception;

import org.springframework.http.HttpStatus;

public class LeaveValidationException extends LeaveModuleException {
    public LeaveValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

