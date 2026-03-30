package com.project.edusync.adm.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends EdusyncException {
    public InvalidRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
