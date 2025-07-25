package com.example.simplechat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.simplechat.dto.ErrorResponseDto;
import com.example.simplechat.exception.RegistrationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ErrorResponseDto> handleRegistrationException(RegistrationException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(ex.getErrorCode(), ex.getMessage());

        HttpStatus status = switch (ex.getErrorCode()) {
            case "DUPLICATE_USERNAME", "DUPLICATE_NICKNAME" -> HttpStatus.CONFLICT; // 409
            case "INVALID_PASSWORD", "INVALID_USERNAME" -> HttpStatus.BAD_REQUEST; // 400
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;	// 401
            default -> HttpStatus.INTERNAL_SERVER_ERROR; // 500
        };

        return new ResponseEntity<>(errorResponse, status);
    }
}
