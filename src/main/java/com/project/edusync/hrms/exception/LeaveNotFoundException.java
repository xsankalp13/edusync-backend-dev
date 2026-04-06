package com.project.edusync.hrms.exception;

import org.springframework.http.HttpStatus;

public class LeaveNotFoundException extends LeaveModuleException {
    public LeaveNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

