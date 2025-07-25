package com.example.simplechat.dto;

public record ErrorResponseDto(
    String errorCode,
    String message
) {}
