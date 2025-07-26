package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage.MsgType;

public record ChatMessageRequestDto(
		Long roomId,
	    Long authorId,
	    String authorName, // User 테이블과 조인해서 가져온 닉네임
	    String content,
	    MsgType messageType    // "TEXT", "IMAGE", "ENTER", "EXIT" 등
) { }
