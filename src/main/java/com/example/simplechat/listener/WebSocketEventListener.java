package com.example.simplechat.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.simplechat.service.SimplechatService;

@Component
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final SimplechatService serv;		// 주입!! 이렇게 하면 알아서 인스턴스를 찾아준다
    public WebSocketEventListener(SimplechatService serv) {
    	this.serv = serv;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();
        Long userid = (Long) headerAccessor.getSessionAttributes().get("user_id");
        Long roomid = (Long) headerAccessor.getSessionAttributes().get("room_id");

        logger.info("WebSocket Session Disconnected: [SessionId: {}], [User: {}], [Reason: {}]",
                sessionId,
                userid != null ? userid : "N/A",
                event.getCloseStatus().getReason());

        // 특정 세션이 끊어졌을 때 필요한 추가 로직을 여기에 구현합니다.
        // 예를 들어, 연결된 사용자 목록에서 제거하거나,
        System.out.println("접속종료: "+roomid+": "+userid);
        serv.exitRoom(userid, roomid);
        
        // 해당 사용자와 관련된 리소스를 정리하는 등의 작업을 할 수 있습니다.
        

        // 만약 사용자 이름이 세션에 저장되어 있다면
        if (userid != null) {
            // 예시: 연결된 사용자 목록에서 제거 (가상의 서비스라고 가정)
            // userService.removeConnectedUser(username);
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