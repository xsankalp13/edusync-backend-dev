package com.project.edusync.adm.exception;

import com.project.edusync.common.exception.EdusyncException;
import org.springframework.http.HttpStatus;

public class TimeSlotException extends EdusyncException {
    public TimeSlotException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
