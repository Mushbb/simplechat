package com.example.simplechat.model;

import java.time.LocalDateTime;

public record Friendship(
    long userId1,
    long userId2,
    String status,
    LocalDateTime createdAt,
    long relationId
) {}
