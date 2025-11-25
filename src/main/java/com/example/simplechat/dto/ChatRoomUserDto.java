package com.example.simplechat.dto;

/**
 * 채팅방 내 사용자 정보를 전송하기 위한 DTO입니다.
 * 사용자의 ID, 닉네임, 역할, 연결 상태 및 프로필 이미지 URL을 포함합니다.
 *
 * @param userId 사용자의 고유 ID
 * @param nickname 사용자의 닉네임
 * @param role 채팅방 내 사용자의 역할 (ADMIN, MEMBER)
 * @param conn 사용자의 연결 상태 (CONNECT, DISCONNECT)
 * @param profileImageUrl 사용자의 프로필 이미지 URL
 */
public record ChatRoomUserDto(
    Long userId,
    String nickname,
    UserType role,
    ConnectType conn,
    String profileImageUrl
) {
    /**
     * 채팅방 내 사용자의 역할을 정의하는 열거형입니다.
     */
    public enum UserType {
        /**
         * 관리자 권한을 가진 사용자입니다.
         */
        ADMIN,
        /**
         * 일반 멤버 사용자입니다.
         */
        MEMBER
    }

    /**
     * 사용자의 연결 상태를 정의하는 열거형입니다.
     */
    public enum ConnectType {
        /**
         * 사용자가 현재 채팅방에 연결되어 있습니다.
         */
        CONNECT,
        /**
         * 사용자가 현재 채팅방에서 연결이 해제되었습니다.
         */
        DISCONNECT
    }
}