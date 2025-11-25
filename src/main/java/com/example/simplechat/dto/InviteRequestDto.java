package com.example.simplechat.dto;

/**
 * 채팅방에 사용자를 초대하기 위한 DTO입니다.
 *
 * @param userId 초대할 사용자의 ID
 */
public record InviteRequestDto(Long userId) {

}