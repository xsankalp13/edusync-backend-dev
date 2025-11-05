package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;

public class FeeTypeNotFoundException extends EdusyncException {
    public FeeTypeNotFoundException(String message) {
        super(message);
    }
}
