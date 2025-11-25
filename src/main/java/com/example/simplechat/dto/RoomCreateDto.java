package com.example.simplechat.dto;

import com.example.simplechat.model.ChatRoom.RoomType;

/**
 * 새로운 채팅방 생성을 위한 DTO입니다.
 *
 * @param roomName 생성할 방의 이름
 * @param roomType 생성할 방의 유형 (예: PUBLIC, PRIVATE)
 * @param password 비공개 방인 경우 설정할 비밀번호 (선택 사항)
 */
public record RoomCreateDto(
    String roomName,
    RoomType roomType,
    String password
) {

}