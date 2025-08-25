package com.example.simplechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration // 이 클래스가 Spring 설정 클래스임을 명시
@EnableWebSocketMessageBroker // STOMP 기반 웹소켓 메시지 브로커 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final UserInterceptor userInterceptor; // UserInterceptor가 있다고 가정

	public WebSocketConfig(UserInterceptor userInterceptor) { // 생성자 주입
		this.userInterceptor = userInterceptor;
	}

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(userInterceptor); // 인터셉터 등록
    }

    /**
     * STOMP WebSocket 연결을 위한 엔드포인트를 등록하고 CORS를 설정합니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080", "http://localhost:3000", "http://10.50.131.25:3000")
                .withSockJS();
    }
}