package com.example.simplechat.dto;

public record NickChangeDto(
		Long roomId,
		Long userId,
		String newNickname
) { }
