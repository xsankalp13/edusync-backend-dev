package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends EdusyncException {
    public InvoiceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
