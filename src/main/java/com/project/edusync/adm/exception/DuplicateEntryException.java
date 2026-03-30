package com.project.edusync.adm.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class DuplicateEntryException extends EdusyncException {
    public DuplicateEntryException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
