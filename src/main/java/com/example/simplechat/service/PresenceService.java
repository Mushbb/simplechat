package com.example.simplechat.service;

import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.dto.PresenceChangeDto;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FriendshipRepository;
import com.example.simplechat.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 사용자의 온라인/오프라인 접속 상태를 추적하고, 친구들에게 접속 상태 변경을 알리는 서비스입니다.
 * WebSocket 세션 연결 및 연결 해제 이벤트를 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class PresenceService {
    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper; // ObjectMapper 주입

    // 접속한 사용자를 관리하는 맵 (Thread-safe한 ConcurrentHashMap 사용)
    // Key: WebSocket Session ID, Value: User ID
    private final Map<String, Long> connectedUsers = new ConcurrentHashMap<>();

    /**
     * 사용자가 웹소켓에 연결되었을 때 실행되는 이벤트 리스너입니다.
     * 세션 정보를 추출하여 연결된 사용자 맵에 추가하고, 친구들에게 접속 상태 변경을 알립니다.
     *
     * @param event 세션 연결 이벤트
     */
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        // room_id가 있는 세션은 채팅방 입장 시 연결되는 세션이므로 presence 추적에서 제외
        if (headerAccessor.getSessionAttributes() != null && headerAccessor.getSessionAttributes().get("room_id") != null) {
            return;
        }
        String sessionId = headerAccessor.getSessionId();
        
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();

        userRepository.findByUsername(username).ifPresent(user -> {
            connectedUsers.put(sessionId, user.getId());
            logger.info("[Presence] 사용자 연결됨: {} (ID: {})", user.getNickname(), user.getId());
            
            notifyPresenceChange(user, true);
        });
    }

    /**
     * 사용자의 웹소켓 연결이 끊겼을 때 실행되는 이벤트 리스너입니다.
     * 연결된 사용자 맵에서 해당 사용자를 제거하고, 친구들에게 접속 상태 변경을 알립니다.
     *
     * @param event 세션 연결 해제 이벤트
     */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        Long userId = connectedUsers.remove(sessionId);

        if (userId != null) {
            logger.info("[Presence] 사용자 연결 해제됨: (ID: {})", userId);
            
            userRepository.findById(userId).ifPresent(user -> {
            	notifyPresenceChange(user, false);
            });
        }
    }

    /**
     * 친구들에게 사용자의 접속 상태 변경을 알립니다.
     * {@link Notification.NotificationType#PRESENCE_UPDATE} 유형의 알림을 각 친구의 개인 큐로 전송합니다.
     *
     * @param user 상태가 변경된 사용자
     * @param isOnline 접속 여부 (true: 온라인, false: 오프라인)
     */
    private void notifyPresenceChange(User user, boolean isOnline) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(user.getId(), Friendship.Status.ACCEPTED);

        try {
            PresenceChangeDto payload = new PresenceChangeDto(user.getId(), user.getNickname(), isOnline);
            String metadata = objectMapper.writeValueAsString(payload); // 주입된 ObjectMapper 사용

            NotificationDto notification = NotificationDto.builder()
                .type(Notification.NotificationType.PRESENCE_UPDATE.name())
                .metadata(metadata)
                .build();

            friendships.forEach(friendship -> {
                long friendId = friendship.getUserId1() == user.getId() ? friendship.getUserId2() : friendship.getUserId1();
                userRepository.findById(friendId).ifPresent(friendUser -> {
                    messagingTemplate.convertAndSendToUser(friendUser.getUsername(), "/queue/notifications", notification);
                });
            });
        } catch (JsonProcessingException e) { // Exception -> JsonProcessingException으로 변경
            logger.error("접속 상태 알림 생성 중 JSON 변환 실패: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("접속 상태 알림 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 특정 사용자가 현재 접속 중인지 확인합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @return 접속 중이면 true, 아니면 false
     */
    public boolean isUserOnline(Long userId) {
        return connectedUsers.containsValue(userId);
    }
}