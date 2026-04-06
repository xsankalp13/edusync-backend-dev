package com.project.edusync.hrms.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class LeaveModuleException extends EdusyncException {
    public LeaveModuleException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    public LeaveModuleException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }
}

