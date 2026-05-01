package com.project.edusync.common.exception.finance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateFeeAssignmentException extends RuntimeException {
    public DuplicateFeeAssignmentException(String message) {
        super(message);
    }
}
