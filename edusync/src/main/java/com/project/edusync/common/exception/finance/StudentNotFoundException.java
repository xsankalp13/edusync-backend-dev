package com.project.edusync.common.exception.finance;

import com.project.edusync.common.exception.EdusyncException;

public class StudentNotFoundException extends EdusyncException {
    public StudentNotFoundException(String message) {
        super(message);
    }
}
