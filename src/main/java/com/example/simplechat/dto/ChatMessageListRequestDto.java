package com.example.simplechat.dto;

public record ChatMessageListRequestDto(
		Long roomId,
		Long beginId,
		Integer rowCount
) { }