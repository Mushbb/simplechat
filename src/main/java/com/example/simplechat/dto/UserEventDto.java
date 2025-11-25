package com.example.simplechat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 사용자 관련 웹소켓 이벤트를 클라이언트에 전달하기 위한 DTO입니다.
 * 클라이언트는 이 DTO의 eventType에 따라 사용자 목록을 다르게 처리할 수 있습니다.
 *
 * @param eventType 이벤트의 종류 (예: ENTER, ROOM_OUT, NICK_CHANGE)
 * @param userId 이벤트 대상 사용자의 고유 ID
 * @param nickname 사용자의 닉네임 (ENTER, NICK_CHANGE 시 사용)
 * @param role 사용자의 역할 (ENTER 시 사용)
 * @param profileImageUrl 사용자의 프로필 이미지 URL (ENTER 시 사용)
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON으로 변환 시 null인 필드는 제외
public record UserEventDto(
    EventType eventType,
    Long userId,
    String nickname,
    UserType role,
    String profileImageUrl
) {
    /**
     * 사용자 이벤트의 종류를 나타내는 열거형입니다.
     */
    public enum EventType {
        /** 새로운 사용자가 채팅방에 입장했음을 알립니다. */
        ENTER,
        /** 기존 사용자가 채팅방에서 나갔음을 알립니다. */
        EXIT,
        /** 사용자가 채팅방에서 퇴장했음을 알립니다. */
        ROOM_OUT,
        /** 채팅방이 삭제되었음을 알립니다. */
        ROOM_DELETED,
        /** 사용자의 닉네임이 변경되었음을 알립니다. */
        NICK_CHANGE,
        /** 사용자의 역할이 변경되었음을 알립니다. */
        ROLE_CHANGE
    }
    
    /**
     * 채팅방 내 사용자의 역할을 정의하는 열거형입니다.
     */
    public enum UserType {
        /** 관리자 권한을 가진 사용자입니다. */
        ADMIN,
        /** 일반 멤버 사용자입니다. */
        MEMBER
    }
}
