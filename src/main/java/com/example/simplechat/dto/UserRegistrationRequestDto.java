package com.example.simplechat.dto;

/**
 * 새로운 사용자 등록 요청을 처리하기 위한 DTO입니다.
 *
 * @param username 사용자명
 * @param password 비밀번호
 * @param nickname 닉네임
 */
public record UserRegistrationRequestDto(
    String username,
    String password,
    String nickname
) {

}
