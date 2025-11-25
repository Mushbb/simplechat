package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage;
import java.util.List;

/**
 * 클라이언트가 서버로 새 채팅 메시지를 보낼 때 사용하는 DTO입니다.
 *
 * @param roomId 메시지를 보낼 방의 ID
 * @param authorId 메시지를 보내는 사용자의 ID
 * @param content 메시지 내용
 * @param messageType 메시지 유형
 * @param mentionedUserIds 메시지에서 멘션된 사용자 ID 목록
 */
public record ChatMessageRequestDto(
    Long roomId,
    Long authorId,
    String content,
    ChatMessage.MsgType messageType,
    List<Long> mentionedUserIds
) {

}
