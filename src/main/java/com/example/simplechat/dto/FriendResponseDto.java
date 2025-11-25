package com.example.simplechat.dto;

import com.example.simplechat.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 친구 정보 및 친구 관계 상태를 클라이언트에 전송하기 위한 DTO입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponseDto {
    /**
     * 친구의 고유 ID입니다.
     */
    private Long userId;
    /**
     * 친구의 사용자명입니다.
     */
    private String username;
    /**
     * 친구의 닉네임입니다.
     */
    private String nickname;
    /**
     * 친구의 프로필 이미지 URL입니다.
     */
    private String profileImageUrl;
    /**
     * 친구 관계의 상태입니다. (예: PENDING_SENT, PENDING_RECEIVED, ACCEPTED)
     */
    private String status;
    /**
     * 친구의 현재 연결 상태입니다.
     */
    private ConnectType conn;

    /**
     * User 엔티티와 친구 관계 상태, 연결 상태를 기반으로 {@link FriendResponseDto} 인스턴스를 생성하는 팩토리 메서드입니다.
     * @param user 친구 User 엔티티
     * @param status 친구 관계 상태 문자열
     * @param conn 친구의 연결 상태
     * @return 생성된 FriendResponseDto 인스턴스
     */
    public static FriendResponseDto from(User user, String status, ConnectType conn) {
        return new FriendResponseDto(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getProfile_image_url(),
            status,
            conn
        );
    }

    /**
     * 친구의 연결 상태를 정의하는 열거형입니다.
     */
    public enum ConnectType {
        /**
         * 친구가 현재 접속 중입니다.
         */
        CONNECT,
        /**
         * 친구가 현재 접속 중이 아닙니다.
         */
        DISCONNECT
    }
}
