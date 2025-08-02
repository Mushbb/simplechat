package com.example.simplechat.dto;

public record UserProfileDto(
		Long userId,
		String username,
		String nickname,
		String status_msg,
		String imageUrl
) {}
