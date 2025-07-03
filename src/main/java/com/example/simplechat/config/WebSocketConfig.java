package com.example.simplechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.scheduling.TaskScheduler; // TaskScheduler import
import org.springframework.web.socket.sockjs.SockJsService; // SockJsService import
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService; // TransportHandlingSockJsService import
// import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler; // 이 import는 더 이상 필요 없습니다.
// import java.util.Collections; // 이 import는 더 이상 필요 없습니다.


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
//              .setHeartbeatValue(new long[]{5000, 5000}) // 예: 10초마다 서버-클라이언트 상호간 heartbeat
//              .setTaskScheduler(heartbeatScheduler()); // Heartbeat를 위한 스케줄러 설정
        config.setApplicationDestinationPrefixes("/app");
    }

    // Heartbeat를 위한 TaskScheduler Bean (STOMP 브로커용)
    // 이 스케줄러는 STOMP 메시지 브로커의 heartbeat를 처리합니다.
//    @Bean
//    // @Primary // @Primary 어노테이션 제거: SockJsService에 명시적으로 주입하므로 필요 없음
//    public ThreadPoolTaskScheduler heartbeatScheduler() {
//        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//        scheduler.setPoolSize(50); // Heartbeat를 위한 스레드 풀 크기
//        scheduler.setThreadNamePrefix("websocket-scheduler-"); // 이름 변경 (하트비트 외 다른 용도로도 사용될 수 있음을 나타냄)
//        scheduler.initialize();
//        return scheduler;
//    }

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
                .setAllowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080")
                .withSockJS();
//                .setHeartbeatTime(5_000) // 5초 heartbeat
//                .setDisconnectDelay(30_000);
    }
}