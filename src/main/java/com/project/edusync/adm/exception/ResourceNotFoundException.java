package com.project.edusync.adm.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends EdusyncException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
