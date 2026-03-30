package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class InvalidPaymentOperationException extends EdusyncException {
    public InvalidPaymentOperationException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
