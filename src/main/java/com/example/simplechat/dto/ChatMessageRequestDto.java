package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage.MsgType;

public record ChatMessageRequestDto(
		Long roomId,
	    Long authorId,
	    String content,
	    MsgType messageType    // "TEXT", "IMAGE", "ENTER", "EXIT" 등
) { }
