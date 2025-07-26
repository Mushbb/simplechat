package com.example.simplechat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 사용자 관련 웹소켓 이벤트를 전달하기 위한 DTO입니다.
 * 클라이언트는 이 DTO의 eventType에 따라 사용자 목록을 다르게 처리합니다.
 *
 * @param eventType 이벤트의 종류 (ENTER, EXIT, NICK_CHANGE)
 * @param userId    이벤트 대상 사용자의 ID
 * @param nickname  사용자의 닉네임 (ENTER, NICK_CHANGE 시 사용)
 * @param role      사용자의 역할 (ENTER 시 사용)
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON으로 변환 시 null인 필드는 제외
public record UserEventDto(
    EventType eventType,
    Long userId,
    String nickname,
    UserType role
) {
    /**
     * 사용자 이벤트의 종류를 나타내는 열거형입니다.
     */
    public enum EventType {
        /** 새로운 사용자가 입장했음을 알립니다. */
        ENTER,
        /** 기존 사용자가 퇴장했음을 알립니다. */
        EXIT,
        ROOM_OUT,
        /** 사용자의 닉네임이 변경되었음을 알립니다. */
        NICK_CHANGE,
        ROLE_CHANGE
    }
    
    public enum UserType {
    	ADMIN,
    	MEMBER
    }
}
