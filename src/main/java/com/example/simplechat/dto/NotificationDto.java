package com.example.simplechat.dto;

import com.example.simplechat.model.Notification;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON으로 변환 시 null인 필드는 제외
public class NotificationDto {
    private Long notificationId;
    private String type;
    private String content;
    private String metadata;
    private boolean isRead; // ✨ 신규: 알림 읽음 상태 추가
    private LocalDateTime createdAt;

    // ✨ 신규: Entity를 DTO로 변환하는 정적 메소드
    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
            .notificationId(notification.getId())
            .type(notification.getNotificationType().name())
            .content(notification.getContent())
            .metadata(notification.getMetadata())
            .isRead(notification.isRead()) // ✨ 신규: isRead 필드 추가
            .createdAt(notification.getCreatedAt())
            .build();
    }
}