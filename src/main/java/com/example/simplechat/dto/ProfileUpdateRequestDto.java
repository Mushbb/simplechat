package com.example.simplechat.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequestDto(
    @Size(max = 255)
    String statusMessage,
    String nickname
) {}