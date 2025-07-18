package com.example.simplechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // 비동기 활성화 어노테이션

import org.springframework.context.annotation.Bean; // 이 임포트 추가
import org.springframework.web.servlet.config.annotation.CorsRegistry; // 이 임포트 추가
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 이 임포트 추가

// import com.example.simplechat.controller.AuthInterceptor;

@SpringBootApplication
@EnableAsync // <-- 이 어노테이션을 추가하여 @Async를 활성화합니다.
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
                        .allowedOriginPatterns("http://10.50.131.*:8000", "http://10.50.131.*:8080", "http://localhost:8000", "http://localhost:8080") // 클라이언트 출처 허용
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // 허용할 HTTP 메서드 (OPTIONS 포함)
                        .allowCredentials(true) // 자격 증명(쿠키 등) 허용
                        .allowedHeaders("*"); // 모든 헤더 허용
            }
            
//            @Override
//            public void addInterceptors(InterceptorRegistry registry) {
//                registry.addInterceptor(new AuthInterceptor())
//                        .order(1)
//                        // 오직 /test/login 경로만 인터셉트 대상에 포함합니다.
//                        // 즉, 사용자가 /test/login POST 요청을 보낼 때만 AuthInterceptor가 실행됩니다.
//                        .addPathPatterns("/test/login")
//                        // excludePathPatterns는 addPathPatterns에 포함된 경로 중에서 제외할 경로를 지정하는 것이므로,
//                        // 현재 설정에서는 불필요합니다.
//                        .excludePathPatterns(); // 아무것도 제외하지 않음
//            }
        };
    }
}