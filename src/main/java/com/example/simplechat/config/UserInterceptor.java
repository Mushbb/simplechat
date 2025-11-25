package com.example.simplechat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 메시지 채널을 가로채서 사용자 정보를 처리하는 인터셉터입니다.
 * <p>
 * 클라이언트가 WebSocket에 연결(CONNECT)할 때, STOMP 헤더에서 사용자 ID와 방 ID를 추출하여
 * WebSocket 세션 속성에 저장하는 역할을 합니다.
 * 이렇게 저장된 정보는 이후의 WebSocket 이벤트(예: 연결 해제) 처리 시 사용됩니다.
 * </p>
 */
@Component
public class UserInterceptor implements ChannelInterceptor {

    /**
     * 메시지가 채널로 전송되기 전에 호출됩니다.
     * <p>
     * STOMP의 CONNECT 명령어일 경우, 네이티브 헤더에서 'user_id'와 'room_id'를 추출하여
     * WebSocket 세션 속성에 저장합니다.
     * </p>
     *
     * @param message 처리할 메시지
     * @param channel 메시지가 전송될 채널
     * @return 수정되거나 원래의 메시지
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader("user_id");
            String roomId = accessor.getFirstNativeHeader("room_id");

            if (userId != null && roomId != null) {
                accessor.getSessionAttributes().put("user_id", userId);
                accessor.getSessionAttributes().put("room_id", roomId);
            }
        }
        return message;
    }
}