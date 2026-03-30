package com.project.edusync.adm.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class AlreadyBookedException extends EdusyncException {
    public AlreadyBookedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
