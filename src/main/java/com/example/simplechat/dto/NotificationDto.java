package com.example.simplechat.dto;

import com.example.simplechat.model.Notification;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클라이언트에 알림 정보를 전송하기 위한 DTO입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON으로 변환 시 null인 필드는 제외
public class NotificationDto {
    /**
     * 알림의 고유 ID입니다.
     */
    private Long notificationId;
    /**
     * 알림의 유형입니다. (예: MENTION, ROOM_INVITATION)
     */
    private String type;
    /**
     * 알림의 내용입니다.
     */
    private String content;
    /**
     * 알림과 관련된 추가 메타데이터 (JSON 문자열 형태).
     */
    private String metadata;
    /**
     * 알림의 읽음 상태입니다. (true: 읽음, false: 안 읽음)
     */
    private boolean isRead;
    /**
     * 알림이 생성된 시간입니다.
     */
    private LocalDateTime createdAt;

    /**
     * {@link Notification} 엔티티를 {@link NotificationDto}로 변환하는 정적 팩토리 메서드입니다.
     *
     * @param notification 변환할 Notification 엔티티
     * @return NotificationDto 인스턴스
     */
    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
            .notificationId(notification.getId())
            .type(notification.getNotificationType().name())
            .content(notification.getContent())
            .metadata(notification.getMetadata())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}