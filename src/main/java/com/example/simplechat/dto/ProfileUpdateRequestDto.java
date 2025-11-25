package com.example.simplechat.dto;

import jakarta.validation.constraints.Size;

/**
 * 사용자 프로필 정보 업데이트를 위한 DTO입니다.
 *
 * @param statusMessage 새 상태 메시지. 최대 255자까지 허용됩니다.
 * @param nickname 새 닉네임
 */
public record ProfileUpdateRequestDto(
    @Size(max = 255)
    String statusMessage,
    String nickname
) {

}