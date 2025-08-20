package com.example.simplechat.dto;

import java.util.List;

public record ChatMessageListDto(
		 Long roomId,
		List<ChatMessageDto> messages
) { }
