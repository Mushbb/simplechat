package com.example.simplechat.dto;

/**
 * 사용자의 온라인/오프라인 상태 변경을 클라이언트에게 알리기 위한 DTO입니다.
 *
 * @param userId 상태가 변경된 사용자의 ID
 * @param nickname 상태가 변경된 사용자의 닉네임
 * @param isOnline 사용자가 현재 온라인 상태인지 여부 (true: 온라인, false: 오프라인)
 */
public record PresenceChangeDto(
    Long userId,
    String nickname,
    boolean isOnline
) {

}