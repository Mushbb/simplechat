package com.example.simplechat.dto;

import java.util.List;

/**
 * 사용자가 채팅방에 입장할 때 필요한 초기 데이터를 클라이언트에 전송하기 위한 DTO입니다.
 *
 * @param users 현재 방에 있는 사용자 목록
 * @param messages 최근 채팅 메시지 목록
 * @param roomName 채팅방의 이름
 */
public record RoomInitDataDto(
    List<ChatRoomUserDto> users,
    List<ChatMessageDto> messages,
    String roomName) {

}