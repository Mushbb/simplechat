package com.example.simplechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean; // 이 임포트 추가
import org.springframework.web.servlet.config.annotation.CorsRegistry; // 이 임포트 추가
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 이 임포트 추가

@SpringBootApplication
public class SimplechatApplication {
	public static void main(String[] args) {
		SpringApplication.run(SimplechatApplication.class, args);
	}
	
	@Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로 (/**)에 대해 CORS 설정 적용
                        .allowedOriginPatterns("http://localhost:8000", "http://localhost:8080") // 클라이언트 출처 허용
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // 허용할 HTTP 메서드 (OPTIONS 포함)
                        .allowCredentials(true) // 자격 증명(쿠키 등) 허용
                        .allowedHeaders("*"); // 모든 헤더 허용
            }
        };
    }
}