package com.example.simplechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // 비동기 활성화 어노테이션
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude ={UserDetailsServiceAutoConfiguration.class})
@EnableAsync // <-- 이 어노테이션을 추가하여 @Async를 활성화합니다.
public class SimplechatApplication {
	public static void main(String[] args) {
		SpringApplication.run(SimplechatApplication.class, args);
	}
}
