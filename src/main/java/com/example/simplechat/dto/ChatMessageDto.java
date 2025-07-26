package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage;

public record ChatMessageDto(
	    Long messageId,
	    Long authorId,
	    String authorName, // User 테이블과 조인해서 가져온 닉네임
	    String content,
	    String messageType,    // "TEXT", "IMAGE", "ENTER", "EXIT" 등
	    String createdAt       // "HH:mm:ss" 등 가공된 시간 문자열
) {
	public ChatMessageDto(ChatMessage entity) {
	    this(
	        entity.getId(),
	        entity.getAuthor_id(),
	        entity.getAuthor_name(),
	        entity.getContent(),
	        entity.getMsg_type().name(),
	        entity.getCreated_at() // 필요시 시간 포맷팅 추가
	    );
	}
}