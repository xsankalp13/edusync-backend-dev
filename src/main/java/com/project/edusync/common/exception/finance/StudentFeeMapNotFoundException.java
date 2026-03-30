package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class StudentFeeMapNotFoundException extends EdusyncException {
    public StudentFeeMapNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
