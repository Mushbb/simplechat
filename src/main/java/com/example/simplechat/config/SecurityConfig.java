package com.example.simplechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 알고리즘을 사용하는 PasswordEncoder를 반환합니다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
            // REST API 서버는 일반적으로 세션 대신 토큰을 사용하므로 CSRF 공격에 덜 취약합니다.
            // 테스트 편의를 위해 비활성화하는 경우가 많습니다.
            .csrf(csrf -> csrf.disable())
            
            // 들어오는 모든 HTTP 요청에 대한 접근 권한을 설정합니다.
            .authorizeHttpRequests(auth -> auth
                // anyRequest() : 어떤 요청이든
                // permitAll() : 인증 없이 접근을 허용합니다.
                // 이 설정을 통해 Spring Security의 기본 인증 요구 사항을 비활성화하여
                // 모든 엔드포인트에 자유롭게 접근할 수 있게 합니다.
                .anyRequest().permitAll() );
                
        return http.build();
    }
}
