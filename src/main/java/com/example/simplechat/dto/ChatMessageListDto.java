package com.example.simplechat.dto;

import java.util.List;

public record ChatMessageListDto(
		List<ChatMessageDto> msgList
) { }
