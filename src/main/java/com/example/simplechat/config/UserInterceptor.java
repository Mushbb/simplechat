package com.example.simplechat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class UserInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 사용자 인증 정보를 추출하여 세션 속성에 저장
            // 예시: WebSocket 연결 시 전송되는 사용자 이름 헤더 (또는 Spring Security 통합)
        	String userid = accessor.getFirstNativeHeader("user_id");
            String roomid = accessor.getFirstNativeHeader("room_id");

            if (userid != null) {
                accessor.getSessionAttributes().put("user_id", userid);
                accessor.getSessionAttributes().put("room_id", roomid);
                // Spring Security를 사용한다면 Principal 객체를 저장
                // accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>()));
            }
        }
        return message;
    }
}