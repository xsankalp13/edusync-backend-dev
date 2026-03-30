package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class PdfGenerationException extends EdusyncException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}

