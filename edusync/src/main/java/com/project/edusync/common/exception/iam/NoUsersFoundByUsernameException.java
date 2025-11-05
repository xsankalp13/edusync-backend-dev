package com.project.edusync.common.exception.iam;

public class NoUsersFoundByUsernameException extends RuntimeException {
    public NoUsersFoundByUsernameException(String message) {
        super(message);
    }
}
