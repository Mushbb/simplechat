package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage;
import java.util.List;

public record ChatMessageRequestDto(
	Long roomId,
	Long authorId,
	String content,
	ChatMessage.MsgType messageType,
	List<Long> mentionedUserIds
) { }
