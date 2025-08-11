package com.example.simplechat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.profile-upload-dir}")
    private String profileUploadDir;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    @Value("${file.chat-upload-dir}")
    private String chatUploadDir;

    @Value("${file.chat-static-url-prefix}")
    private String chatStaticUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로필 이미지 경로 설정
        registry.addResourceHandler(profileStaticUrlPrefix + "/**")
                .addResourceLocations("file:" + profileUploadDir + "/", "classpath:/uploads/profiles/");

        // 채팅 파일 경로 설정
        registry.addResourceHandler(chatStaticUrlPrefix + "/**")
                .addResourceLocations("file:" + chatUploadDir + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로 (/**)에 대해 CORS 설정 적용
                .allowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080", "http://localhost:3000") // 클라이언트 출처 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // 허용할 HTTP 메서드 (OPTIONS 포함)
                .allowCredentials(true) // 자격 증명(쿠키 등) 허용
                .allowedHeaders("*"); // 모든 헤더 허용
    }
}
