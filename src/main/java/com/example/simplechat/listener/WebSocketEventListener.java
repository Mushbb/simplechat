package com.example.simplechat.listener;

import com.example.simplechat.dto.UserEventDto.EventType;
import com.example.simplechat.dto.UserEventDto.UserType;
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.service.RoomSessionManager;
import com.example.simplechat.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket 관련 이벤트를 수신하고 처리하는 리스너 클래스입니다.
 * 사용자의 구독 및 연결 해제 이벤트를 감지하여 적절한 비즈니스 로직을 트리거합니다.
 */
@RequiredArgsConstructor
@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomSessionManager roomSessionManager;
    private final RoomUserRepository roomUserRepository;

    /**
     * 사용자가 특정 STOMP 토픽을 구독할 때 발생하는 이벤트를 처리합니다.
     * 사용자가 채팅방의 public 토픽을 구독하면, 이를 채팅방 입장으로 간주하고 {@link UserEnteredRoomEvent}를 발행합니다.
     *
     * @param event 세션 구독 이벤트
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes == null || sessionAttributes.get("room_id") == null) {
        	return;
        }
        String destination = headerAccessor.getDestination();

        // 사용자가 /topic/{roomId}/public 토픽을 구독할 때를 입장 시점으로 간주
        if (destination != null && destination.contains("/topic/") && destination.contains("/public")) {
        	Long userId = Long.valueOf((String)sessionAttributes.get("user_id"));
            Long roomId = Long.valueOf((String)sessionAttributes.get("room_id"));
            User user = userService.getUserById(userId);

            if (userId != null && roomId != null && user != null) {
            	roomSessionManager.registerSession(roomId, userId, headerAccessor.getSessionId());
                // 여기서 UserEnteredRoomEvent 발행
            	eventPublisher.publishEvent(new UserEnteredRoomEvent(this, user, roomId, 
            			UserType.valueOf(roomUserRepository.getRole(userId, roomId))) );
                logger.info("사용자 {}가 방 {}을(를) 구독했습니다. 입장 이벤트를 발행합니다.", userId, roomId);
            }
        }
    }
    
    /**
     * 사용자의 WebSocket 연결이 끊어졌을 때 발생하는 이벤트를 처리합니다.
     * 세션 정보를 기반으로 해당 사용자가 어떤 방에 있었는지 확인하고, 있었던 경우 {@link UserExitedRoomEvent}를 발행합니다.
     *
     * @param event 세션 연결 해제 이벤트
     */
    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        
        if (sessionAttributes == null) {
            return;
        }

        String sessionId = headerAccessor.getSessionId();
        String userIdStr = (String) sessionAttributes.get("user_id");
        String roomIdStr = (String) sessionAttributes.get("room_id");

        logger.info("WebSocket 세션 연결 해제됨: [세션 ID: {}], [사용자 ID: {}], [방 ID: {}], [이유: {}]",
                sessionId,
                userIdStr != null ? userIdStr : "N/A",
                roomIdStr != null ? roomIdStr : "N/A",
                event.getCloseStatus().getReason());

        // 채팅방 세션인 경우에만 퇴장 로직 처리
        if (userIdStr != null && roomIdStr != null) {
            try {
                Long userId = Long.valueOf(userIdStr);
                Long roomId = Long.valueOf(roomIdStr);
                
                roomSessionManager.unregisterSession(sessionId);
                // 퇴장 이벤트 발행
                eventPublisher.publishEvent(new UserExitedRoomEvent(this, userId, roomId, EventType.EXIT));
                logger.info("사용자 {}의 연결이 끊어졌습니다. 리소스를 정리합니다.", userId);
            } catch (NumberFormatException e) {
                logger.error("세션 속성 'user_id' 또는 'room_id'를 Long으로 변환하는 데 실패했습니다.", e);
            }
        }
    }
}