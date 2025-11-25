package com.example.simplechat.dto;

/**
 * 사용자 프로필 정보를 클라이언트에 전송하기 위한 DTO입니다.
 *
 * @param userId 사용자의 고유 ID
 * @param username 사용자의 사용자명
 * @param nickname 사용자의 닉네임
 * @param status_msg 사용자의 상태 메시지
 * @param imageUrl 사용자의 프로필 이미지 URL
 */
public record UserProfileDto(
    Long userId,
    String username,
    String nickname,
    String status_msg,
    String imageUrl
) {

}
