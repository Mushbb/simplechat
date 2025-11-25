package com.example.simplechat.dto;

/**
 * 채팅방 입장을 위한 DTO입니다.
 * 주로 비공개 방에 입장할 때 비밀번호를 전달하는 데 사용됩니다.
 *
 * @param password 비공개 방인 경우 필요한 비밀번호 (선택 사항)
 */
public record RoomEnterDto(String password) {

}