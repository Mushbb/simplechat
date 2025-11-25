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

/**
 * 애플리케이션의 보안 관련 설정을 담당하는 클래스입니다.
 * Spring Security를 사용하여 웹 기반 보안, CORS 정책, 비밀번호 암호화 등을 구성합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder를 빈으로 등록합니다.
     * BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해싱합니다.
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP 요청에 대한 보안 필터 체인을 구성합니다.
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts.disable()) // HSTS 비활성화
                )
            .authorizeHttpRequests(authz -> authz
        		.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()

        	    // CORS Preflight 요청은 항상 허용
        	    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

        	    // 인증이 필요없는 경로
        	    .requestMatchers("/auth/login", "/auth/register").permitAll()
        	    .requestMatchers(HttpMethod.GET, "/room/list").permitAll()
        	    .requestMatchers("/ws/**").permitAll()

        	    .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
        	);

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 구성합니다.
     * 특정 출처에서의 요청을 허용하고, 허용할 HTTP 메서드 및 헤더를 정의합니다.
     * @return CorsConfigurationSource 인스턴스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList("http://10.50.131.25:8000", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 위 설정 적용
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
