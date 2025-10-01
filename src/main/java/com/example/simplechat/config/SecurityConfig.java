package com.example.simplechat.config;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
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
            .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts.disable()) // 🔴 HSTS 비활성화
                )
            .authorizeHttpRequests(authz -> authz
        		.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
            		
        	    // CORS Preflight 요청은 항상 허용
        	    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

        	    // --- 👇 인증 없이 접근해야만 하는 경로들 ---
        	    .requestMatchers("/auth/login", "/auth/register").permitAll()
        	    .requestMatchers(HttpMethod.GET, "/room/list").permitAll()
        	    .requestMatchers("/ws/**").permitAll()

        	    // --- 👇 그 외 모든 요청은 반드시 인증 필요 ---
        	    .anyRequest().authenticated() // '/auth/session' 포함 모든 요청은 인증된 사용자만
        	);

        return http.build();
    }
 // ✅ 2. CORS 상세 설정을 정의합니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList("http://10.50.131.25:8000", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 위 CORS 설정을 적용합니다.
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
