package com.project.edusync.common.exception.emException;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A custom, UNCHECKED exception for the entire application.
 * All module-specific exceptions (like ExamNotFoundException)
 * will extend this class.
 */
@Getter
public class EdusyncException extends com.project.edusync.common.exception.EdusyncException {

    private final String errorCode;

    public EdusyncException(String errorCode, String message, HttpStatus httpStatus) {
        super(message, httpStatus);
        this.errorCode = errorCode;
    }

    public EdusyncException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
        this.errorCode = errorCode;
    }
}