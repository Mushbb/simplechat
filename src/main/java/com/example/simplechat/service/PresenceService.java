package com.example.simplechat.service;

import com.example.simplechat.model.User;
import com.example.simplechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PresenceService {

    private final UserRepository userRepository;

    // 접속한 사용자를 관리하는 맵 (Thread-safe한 ConcurrentHashMap 사용)
    // Key: WebSocket Session ID, Value: User ID
    private final Map<String, Long> connectedUsers = new ConcurrentHashMap<>();

    /**
     * 사용자가 웹소켓에 연결되었을 때 실행되는 이벤트 리스너
     */
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // SecurityContext에서 인증된 사용자 정보(Principal)를 가져옴
        // Principal의 이름은 보통 username 입니다.
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();

        // username으로 DB에서 User 정보를 찾아 ID를 얻음
        userRepository.findByUsername(username).ifPresent(user -> {
            connectedUsers.put(sessionId, user.getId());
            System.out.println("[Presence] User Connected: " + user.getNickname() + " (ID: " + user.getId() + ")");
        });
    }

    /**
     * 사용자의 웹소켓 연결이 끊겼을 때 실행되는 이벤트 리스너
     */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 맵에서 해당 세션 ID를 가진 사용자를 제거
        Long userId = connectedUsers.remove(sessionId);

        if (userId != null) {
            System.out.println("[Presence] User Disconnected: (ID: " + userId + ")");
        }
    }

    /**
     * 특정 사용자가 현재 접속 중인지 확인하는 메서드
     * @param userId 확인할 사용자의 ID
     * @return 접속 중이면 true, 아니면 false
     */
    public boolean isUserOnline(Long userId) {
        // connectedUsers 맵의 value(userId) 중에 해당 ID가 있는지 확인
        return connectedUsers.containsValue(userId);
    }
}