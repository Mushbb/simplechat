package com.example.simplechat.dto;

public record LoginResponseDto(
    Long userId,
    String username,
    String nickname
) {}
