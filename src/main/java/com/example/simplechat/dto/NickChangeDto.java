package com.example.simplechat.dto;

/**
 * 특정 채팅방에서 사용자의 닉네임 변경을 요청하기 위한 DTO입니다.
 *
 * @param roomId 닉네임을 변경할 방의 ID
 * @param userId 닉네임을 변경하는 사용자의 ID
 * @param newNickname 새로 설정할 닉네임
 */
public record NickChangeDto(
    Long roomId,
    Long userId,
    String newNickname
) {

}
