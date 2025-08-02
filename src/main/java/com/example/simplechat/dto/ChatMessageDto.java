package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage;

public record ChatMessageDto(
	    Long messageId,
	    Long authorId,
	    String authorName, // User 테이블과 조인해서 가져온 닉네임
	    String authorProfileImageUrl,
	    String content,
	    String messageType,    // "TEXT", "IMAGE", "ENTER", "EXIT" 등
	    String createdAt       // "HH:mm:ss" 등 가공된 시간 문자열
) {
	public ChatMessageDto(ChatMessage entity, String profileImageUrl) {
	    this(
	        entity.getId(),
	        entity.getAuthor_id(),
	        entity.getAuthor_name(),
	        profileImageUrl, // 추가된 프로필 이미지 URL
	        entity.getContent(),
	        entity.getMsg_type().name(),
	        entity.getCreated_at()
	    );
	}
}