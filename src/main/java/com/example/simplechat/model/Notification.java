package com.example.simplechat.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    // ✨ 신규: NotificationType Enum을 내부 클래스로 정의
    public enum NotificationType {
        FRIEND_REQUEST,
        ROOM_INVITATION,
        PRESENCE_UPDATE,
		FRIEND_ACCEPTED,
		FRIEND_ADDED
    }

    private Long id;
    private Long receiverId;
    private NotificationType notificationType;
    private String content; // 프론트에 표시될 기본 메시지
    private Long relatedEntityId; // 친구 요청 시: 요청자 ID, 방 초대 시: 방 ID
    private String metadata; // 추가 정보 (예: 방 초대 시 초대한 사람 닉네임)
    private boolean isRead;
    private LocalDateTime createdAt;

    // save 시 사용할 생성자
    public Notification(Long receiverId, NotificationType type, String content, Long relatedEntityId, String metadata) {
        this.receiverId = receiverId;
        this.notificationType = type;
        this.content = content;
        this.relatedEntityId = relatedEntityId;
        this.metadata = metadata;
        this.isRead = false;
    }
}