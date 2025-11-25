package com.example.simplechat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 및 STOMP 메시징 프로토콜을 위한 설정 클래스입니다.
 * STOMP 기반의 메시지 브로커 기능을 활성화하고, 엔드포인트 및 메시지 채널을 구성합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserInterceptor userInterceptor;

    public WebSocketConfig(UserInterceptor userInterceptor) {
        this.userInterceptor = userInterceptor;
    }

    /**
     * 메시지 브로커를 구성합니다.
     * <ul>
     *     <li>enableSimpleBroker: /topic, /queue 접두사를 사용하는 간단한 인메모리 브로커를 활성화합니다.
     *     클라이언트가 이 경로들을 구독할 수 있습니다.</li>
     *     <li>setApplicationDestinationPrefixes: @MessageMapping 메서드로 라우팅될 메시지의 접두사를 /app으로 설정합니다.</li>
     * </ul>
     * @param config 메시지 브로커 설정을 위한 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 클라이언트로부터 들어오는 인바운드 채널을 구성합니다.
     * 여기서는 사용자 정보를 처리하기 위한 커스텀 인터셉터를 등록합니다.
     * @param registration 채널 등록을 위한 객체
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(userInterceptor);
    }

    /**
     * STOMP WebSocket 연결을 위한 엔드포인트를 등록하고 CORS를 설정합니다.
     * 클라이언트는 /ws 엔드포인트를 통해 WebSocket 연결을 시작할 수 있습니다.
     * SockJS는 WebSocket을 지원하지 않는 브라우저를 위한 대체 옵션으로 활성화됩니다.
     * @param registry STOMP 엔드포인트를 등록하기 위한 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://10.50.131.25:8000", "http://localhost:3000")
                .withSockJS();
    }
}