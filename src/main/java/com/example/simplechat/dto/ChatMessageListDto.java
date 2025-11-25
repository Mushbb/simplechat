package com.example.simplechat.dto;

import java.util.List;

/**
 * 특정 채팅방의 메시지 목록을 클라이언트에게 전송하기 위한 DTO입니다.
 *
 * @param roomId 메시지가 속한 방의 ID
 * @param messages 채팅 메시지 DTO 목록
 */
public record ChatMessageListDto(
    Long roomId,
    List<ChatMessageDto> messages
) {

}
