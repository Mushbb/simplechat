package com.example.simplechat.dto;

public record PresenceChangeDto(Long userId, String nickname, boolean isOnline) {}