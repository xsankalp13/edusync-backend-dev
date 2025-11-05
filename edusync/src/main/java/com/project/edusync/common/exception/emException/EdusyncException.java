package com.project.edusync.common.exception.emException;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A custom, UNCHECKED exception for the entire application.
 * All module-specific exceptions (like ExamNotFoundException)
 * will extend this class.
 */
@Getter
public class EdusyncException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public EdusyncException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public EdusyncException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}