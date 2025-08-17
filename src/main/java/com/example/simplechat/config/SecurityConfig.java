package com.example.simplechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 알고리즘을 사용하는 PasswordEncoder를 반환합니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // 모든 OPTIONS 사전 요청은 무조건 허용 (CORS 문제 해결)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // --- 👇 인증 없이 접근 허용할 경로들 ---
                .requestMatchers("/auth/**").permitAll()
                
                // ✅ 이 줄이 가장 중요합니다. GET 방식의 /room/list 요청은 무조건 허용!
                .requestMatchers(HttpMethod.GET, "/room/list").permitAll() 
                
                .requestMatchers("/ws/**").permitAll()
                // --- 그 외 모든 요청 (React Router가 처리) ---
                .anyRequest().permitAll()
            );

        return http.build();
    }

 // ✅ 2. CORS 상세 설정을 정의합니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // React 개발 서버 주소(localhost:3000)를 허용합니다.
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // 모든 HTTP 메소드(GET, POST, PUT, DELETE 등)를 허용합니다.
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 모든 요청 헤더를 허용합니다.
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 쿠키/세션을 포함한 요청을 허용합니다.
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 위 CORS 설정을 적용합니다.
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
