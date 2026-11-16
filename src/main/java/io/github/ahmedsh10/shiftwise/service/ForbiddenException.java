package io.github.ahmedsh10.shiftwise.service;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
