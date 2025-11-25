package com.example.simplechat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 설정을 사용자 정의하는 클래스입니다.
 * 정적 리소스 핸들러와 전역 CORS(Cross-Origin Resource Sharing) 정책을 구성합니다.
 */
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

    /**
     * 정적 리소스 핸들러를 추가하여 특정 URL 경로 요청 시 파일 시스템의 리소스를 제공하도록 설정합니다.
     * <ul>
     *     <li>프로필 이미지: /profiles/** URL을 통해 file:D:/uploads/profiles/ 경로의 파일을 제공합니다.</li>
     *     <li>채팅 파일: /chat-files/** URL을 통해 file:D:/uploads/chat-files/ 경로의 파일을 제공합니다.</li>
     * </ul>
     * @param registry 리소스 핸들러를 등록하는 데 사용되는 레지스트리
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(profileStaticUrlPrefix + "/**")
                .addResourceLocations("file:" + profileUploadDir + "/", "classpath:/uploads/profiles/");

        registry.addResourceHandler(chatStaticUrlPrefix + "/**")
                .addResourceLocations("file:" + chatUploadDir + "/");
    }

    /**
     * 애플리케이션 전반에 걸쳐 적용될 CORS 정책을 설정합니다.
     * SecurityConfig의 CORS 설정과 함께 사용될 수 있으며, 보다 세부적인 제어를 위해 양쪽 모두에 설정할 수 있습니다.
     * @param registry CORS 설정을 등록하는 데 사용되는 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 설정 적용
                .allowedOrigins("http://10.50.131.25:8000", "http://localhost:3000") // 클라이언트 출처 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // 허용할 HTTP 메서드
                .allowCredentials(true) // 자격 증명(쿠키 등) 허용
                .allowedHeaders("*"); // 모든 헤더 허용
    }
}
