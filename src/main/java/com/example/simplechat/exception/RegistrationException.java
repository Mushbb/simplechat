package com.example.simplechat.exception;

import lombok.Getter;

@Getter
public class RegistrationException extends RuntimeException {
    private final String errorCode;

    public RegistrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
