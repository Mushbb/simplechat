package com.example.simplechat.event;

import com.example.simplechat.model.ChatMessage;
import org.springframework.context.ApplicationEvent;

/**
 * 채팅 메시지가 방에 추가, 수정 또는 삭제되었을 때 발행되는 애플리케이션 이벤트입니다.
 * 이 이벤트를 통해 관련 컴포넌트(예: WebSocket 리스너)들이 메시지 변경을 감지하고
 * 클라이언트에게 실시간으로 업데이트를 전송할 수 있습니다.
 */
public class ChatMessageAddedToRoomEvent extends ApplicationEvent {
    private final ChatMessage chatMessage;
    private final Long roomId;

    /**
     * 새로운 ChatMessageAddedToRoomEvent를 생성합니다.
     *
     * @param source 이벤트를 발생시킨 객체 (일반적으로 `this`)
     * @param chatMessage 추가/수정/삭제된 {@link ChatMessage} 객체
     * @param roomId 메시지가 속한 방의 ID
     */
    public ChatMessageAddedToRoomEvent(Object source, ChatMessage chatMessage, Long roomId) {
        super(source);
        this.chatMessage = chatMessage;
        this.roomId = roomId;
    }

    /**
     * 이벤트와 관련된 {@link ChatMessage} 객체를 반환합니다.
     * @return {@link ChatMessage} 객체
     */
    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    /**
     * 메시지가 속한 방의 ID를 반환합니다.
     * @return 방의 ID
     */
    public Long getroomId() {
        return roomId;
    }

    /**
     * 이벤트 정보를 문자열 형태로 반환합니다.
     * @return 이벤트 정보 문자열
     */
    @Override
    public String toString() {
        return "ChatMessageAddedToRoomEvent{" +
               "chatMessage=" + chatMessage +
               ", roomName='" + roomId + '\'' +
               '}';
    }
}