package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class InvoiceAlreadyPaidException extends EdusyncException {
    public InvoiceAlreadyPaidException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
