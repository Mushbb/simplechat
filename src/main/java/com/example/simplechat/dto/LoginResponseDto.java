package com.example.simplechat.dto;

/**
 * 사용자 로그인 성공 후 클라이언트에 전송되는 응답 데이터를 위한 DTO입니다.
 *
 * @param userId 로그인한 사용자의 고유 ID
 * @param username 로그인한 사용자의 사용자명
 * @param nickname 로그인한 사용자의 닉네임
 */
public record LoginResponseDto(
    Long userId,
    String username,
    String nickname
) {

}
