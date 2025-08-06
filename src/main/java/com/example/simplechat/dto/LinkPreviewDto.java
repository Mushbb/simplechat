package com.example.simplechat.dto;

public record LinkPreviewDto(
    Long messageId,
    String url,
    String title,
    String description,
    String imageUrl
) {}
