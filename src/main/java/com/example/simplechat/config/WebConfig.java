package com.example.simplechat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL 경로가 /images/** 로 시작하는 모든 요청을
        // 실제 파일 시스템 경로인 file:./uploads/profiles/ 와 매핑합니다.
        registry.addResourceHandler("/images/profiles/**")
                .addResourceLocations("file:" + uploadDir + "/", "classpath:/uploads/profiles/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로 (/**)에 대해 CORS 설정 적용
                .allowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080") // 클라이언트 출처 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // 허용할 HTTP 메서드 (OPTIONS 포함)
                .allowCredentials(true) // 자격 증명(쿠키 등) 허용
                .allowedHeaders("*"); // 모든 헤더 허용
    }
}
