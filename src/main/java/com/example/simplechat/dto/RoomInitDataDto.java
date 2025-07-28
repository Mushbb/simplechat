package com.example.simplechat.dto;

import java.util.List;

public record RoomInitDataDto(
		List<ChatRoomUserDto> users,
		List<ChatMessageDto> messages,
		String roomName) { }