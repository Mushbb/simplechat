package com.example.simplechat.dto;

import com.example.simplechat.model.ChatRoom.RoomType;

public record RoomCreateDto(
	String roomName,
	RoomType roomType,
	String password
) {}