package com.example.simplechat.listener;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;

import com.example.simplechat.service.UserService;
import com.example.simplechat.model.User;
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.service.RoomSessionManager;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.dto.UserEventDto.EventType;
import com.example.simplechat.dto.UserEventDto.UserType;

@RequiredArgsConstructor
@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final UserService userService;		// 주입!! 이렇게 하면 알아서 인스턴스를 찾아준다
    private final ApplicationEventPublisher eventPublisher; // Spring의 ApplicationEventPublisher 주입
    private final RoomSessionManager roomSessionManager;
    private final RoomUserRepository roomUserRepository;

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if( headerAccessor.getSessionAttributes().get("room_id") == null )
        	return;
        String destination = headerAccessor.getDestination();

        // 사용자가 /topic/{roomId}/public 또는 /users 토픽을 구독할 때를 입장 시점으로 간주
        if (destination != null && destination.contains("/topic/") && destination.contains("/public")) {
        	Long userId = Long.valueOf((String)headerAccessor.getSessionAttributes().get("user_id"));
            Long roomId = Long.valueOf((String)headerAccessor.getSessionAttributes().get("room_id"));
            User user = userService.getUserById(userId);

            if (userId != null && roomId != null && user != null) {
            	roomSessionManager.registerSession(roomId, userId, headerAccessor.getSessionId());
                // 여기서 UserEnteredRoomEvent 발행
            	eventPublisher.publishEvent(new UserEnteredRoomEvent(this, user, roomId, 
            			UserType.valueOf(roomUserRepository.getRole(userId, roomId))) );
                logger.info("User {} subscribed to room {}. Publishing enter event.", userId, roomId);
            }
        }
    }
    
    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String sessionId = headerAccessor.getSessionId();
        Long userid = Long.valueOf((String)headerAccessor.getSessionAttributes().get("user_id"));
        Long roomid = Long.valueOf((String)headerAccessor.getSessionAttributes().get("room_id"));

        logger.info("WebSocket Session Disconnected: [SessionId: {}], [User: {}], [Reason: {}]",
                sessionId,
                userid != null ? userid : "N/A",
                event.getCloseStatus().getReason());

        // 특정 세션이 끊어졌을 때 필요한 추가 로직을 여기에 구현합니다.
        // 예를 들어, 연결된 사용자 목록에서 제거하거나,
        System.out.println("접속종료: "+roomid+": "+userid);
        
        // 만약 사용자 이름이 세션에 저장되어 있다면
        if (userid != null) {
        	roomSessionManager.unregisterSession(sessionId);
        	// 여기서 UserEnteredRoomEvent 발행
        	eventPublisher.publishEvent(new UserExitedRoomEvent(this, userid, roomid, EventType.EXIT));
            logger.info("User {} disconnected. Cleaning up resources.", userid);
        }
    }

    // 선택 사항: 연결 시 이벤트 처리
    // @EventListener
    // public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    //     StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    //     String sessionId = headerAccessor.getSessionId();
    //     String username = (String) headerAccessor.getSessionAttributes().get("username"); // 연결 시 저장된 사용자 이름
    //     logger.info("WebSocket Session Connected: [SessionId: {}], [User: {}]", sessionId, username != null ? username : "N/A");
    //     // 연결 시 필요한 로직 (예: 사용자 이름 세션에 저장 등)
    // }
}