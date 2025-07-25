package com.example.simplechat.dto;

public record UserRegistrationRequestDto(
    String username,
    String password,
    String nickname
) {}
