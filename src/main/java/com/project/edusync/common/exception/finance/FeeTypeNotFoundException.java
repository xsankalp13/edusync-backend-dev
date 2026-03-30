package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class FeeTypeNotFoundException extends EdusyncException {
    public FeeTypeNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
