package com.example.simplechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration // 이 클래스가 Spring 설정 클래스임을 명시
@EnableWebSocketMessageBroker // STOMP 기반 웹소켓 메시지 브로커 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final UserInterceptor userInterceptor;
	
	public WebSocketConfig(UserInterceptor userInterceptor) { // 생성자 주입
		this.userInterceptor = userInterceptor;
	}
	
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{10000, 10000}) // 예: 10초마다 서버-클라이언트 상호간 heartbeat
        .setTaskScheduler(heartbeatScheduler()); // Heartbeat를 위한 스케줄러 설정
        config.setApplicationDestinationPrefixes("/app");
    }

    // Heartbeat를 위한 TaskScheduler Bean
    // 이 스케줄러는 STOMP 메시지 브로커의 hWWeartbeat를 처리합니다.
    // ⭐️ 중요: TaskScheduler 빈을 명시적으로 정의하고 @Primary 또는 'taskExecutor' 이름 지정
    @Bean
    @Primary // 또는 @Bean(name = "taskExecutor")
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20); // Heartbeat를 위한 스레드 풀 크기
        scheduler.setThreadNamePrefix("websocket-heartbeat-scheduler-");
        scheduler.initialize();
        return scheduler;
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
                .setAllowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080") // 8000 포트 허용
                .withSockJS();
    }
}