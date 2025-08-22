package com.example.simplechat.service;

import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.dto.PresenceChangeDto;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.UserRepository;
import com.example.simplechat.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PresenceService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository; // 👈 친구 관계 조회를 위해 추가
    private final SimpMessagingTemplate messagingTemplate; // 👈 메시지 전송을 위해 추가

    // 접속한 사용자를 관리하는 맵 (Thread-safe한 ConcurrentHashMap 사용)
    // Key: WebSocket Session ID, Value: User ID
    private final Map<String, Long> connectedUsers = new ConcurrentHashMap<>();

    /**
     * 사용자가 웹소켓에 연결되었을 때 실행되는 이벤트 리스너
     */
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if( headerAccessor.getSessionAttributes().get("room_id") != null )
        	return;
        String sessionId = headerAccessor.getSessionId();
        
        // SecurityContext에서 인증된 사용자 정보(Principal)를 가져옴
        // Principal의 이름은 보통 username 입니다.
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();

        // username으로 DB에서 User 정보를 찾아 ID를 얻음
        userRepository.findByUsername(username).ifPresent(user -> {
            connectedUsers.put(sessionId, user.getId());
            System.out.println("[Presence] User Connected: " + user.getNickname() + " (ID: " + user.getId() + ")");
            
            notifyPresenceChange(user, true);
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
            
            userRepository.findById(userId).ifPresent(user -> {
            	notifyPresenceChange(user, false);
            });
        }
    }

    /**
     * 친구들에게 사용자의 접속 상태 변경을 알립니다.
     * @param user 상태가 변경된 사용자
     * @param isOnline 접속 여부
     */
    private void notifyPresenceChange(User user, boolean isOnline) {
        // 1. 상태가 변경된 사용자의 친구 목록을 가져옵니다.
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(user.getId(), Friendship.Status.ACCEPTED);
        
        // 2. 알림 메시지 DTO를 생성합니다.
        PresenceChangeDto payload = new PresenceChangeDto(user.getId(), user.getNickname(), isOnline);
        NotificationDto<PresenceChangeDto> notification = new NotificationDto<>("PRESENCE_UPDATE", payload);

        // 3. 각 친구에게 개인 큐로 알림을 보냅니다.
        friendships.forEach(friendship -> {
            long friendId = friendship.getUserId1() == user.getId() ? friendship.getUserId2() : friendship.getUserId1();
            userRepository.findById(friendId).ifPresent(friendUser -> {
                messagingTemplate.convertAndSendToUser(friendUser.getUsername(), "/queue/notifications", notification);
            });
        });
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