package com.example.simplechat.event;

import com.example.simplechat.model.ChatMessage; // ChatMessage 클래스 import
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class ChatMessageAddedToRoomEvent extends ApplicationEvent {
    private final ChatMessage chatMessage;
    private final Long roomId;

    // ApplicationEvent는 이벤트를 발행한 소스(source) 객체를 생성자에 받도록 요구합니다.
    public ChatMessageAddedToRoomEvent(Object source, ChatMessage chatMessage, Long roomId) {
        super(source); // 이벤트를 발생시킨 객체 (여기서는 ChatRoom 인스턴스)
        this.chatMessage = chatMessage;
        this.roomId = roomId;
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    public Long getroomId() {
        return roomId;
    }

    @Override
    public String toString() {
        return "ChatMessageAddedToRoomEvent{" +
               "chatMessage=" + chatMessage +
               ", roomName='" + roomId + '\'' +
               '}';
    }
}