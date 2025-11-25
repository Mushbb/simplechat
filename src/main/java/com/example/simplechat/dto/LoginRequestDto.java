package com.example.simplechat.dto;

/**
 * 사용자 로그인 요청을 처리하기 위한 DTO입니다.
 *
 * @param username 사용자명
 * @param password 비밀번호
 */
public record LoginRequestDto(
    String username,
    String password
) {

}
