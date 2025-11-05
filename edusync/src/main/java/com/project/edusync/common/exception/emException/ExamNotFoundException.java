package com.project.edusync.common.exception.emException;


import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * A specific exception thrown when an Exam entity cannot be found.
 */
public class ExamNotFoundException extends EdusyncException {

    /**
     * Creates a new ExamNotFoundException.
     * @param uuid The UUID of the exam that was not found.
     */
    public ExamNotFoundException(UUID uuid) {
        super(
                "EM-404-01", // A unique error code for this specific case
                "Exam not found with UUID: " + uuid,
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Overloaded constructor for other exam-related not-found scenarios.
     * @param message A custom message.
     */
    public ExamNotFoundException(String message) {
        super(
                "EM-404-02",
                message,
                HttpStatus.NOT_FOUND
        );
    }
}