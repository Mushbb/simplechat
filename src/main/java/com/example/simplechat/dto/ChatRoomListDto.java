package com.example.simplechat.dto;

public record ChatRoomListDto(
    Long id,
    String name,
    String roomType,
    String ownerName,
    Integer userCount
) {}
