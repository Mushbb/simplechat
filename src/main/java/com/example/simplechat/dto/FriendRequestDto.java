package com.example.simplechat.dto;

/**
 * 친구 요청을 보내기 위한 DTO입니다.
 *
 * @param receiverId 친구 요청을 받을 사용자의 ID
 */
public record FriendRequestDto(
    Long receiverId
) {

}