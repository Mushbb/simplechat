package com.example.simplechat.dto;

import com.example.simplechat.model.ChatMessage;

/**
 * 클라이언트에 전송될 채팅 메시지의 데이터 전송 객체(DTO)입니다.
 *
 * @param messageId 메시지 고유 ID
 * @param authorId 작성자 ID
 * @param authorName 작성자 닉네임
 * @param authorProfileImageUrl 작성자 프로필 이미지 URL
 * @param content 메시지 내용
 * @param messageType 메시지 유형 (e.g., "TEXT", "IMAGE")
 * @param createdAt 메시지 생성 시간 (가공된 문자열)
 */
public record ChatMessageDto(
    Long messageId,
    Long authorId,
    String authorName,
    String authorProfileImageUrl,
    String content,
    String messageType,
    String createdAt
) {
    /**
     * {@link ChatMessage} 엔티티와 프로필 이미지 URL을 사용하여 {@link ChatMessageDto}를 생성하는 생성자입니다.
     *
     * @param entity          채팅 메시지 엔티티
     * @param profileImageUrl 사용자의 프로필 이미지 URL
     */
    public ChatMessageDto(ChatMessage entity, String profileImageUrl) {
        this(
            entity.getId(),
            entity.getAuthor_id(),
            entity.getAuthor_name(),
            profileImageUrl,
            entity.getContent(),
            entity.getMsg_type().name(),
            entity.getCreated_at()
        );
    }
}