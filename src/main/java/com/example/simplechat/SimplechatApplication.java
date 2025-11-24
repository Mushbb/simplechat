package com.example.simplechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // 비동기 활성화 어노테이션
import org.springframework.scheduling.annotation.EnableScheduling; // 스케줄링 활성화 어노테이션
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.simplechat.repository.FileRepository;


@SpringBootApplication(exclude ={UserDetailsServiceAutoConfiguration.class})
@EnableAsync // <-- 이 어노테이션을 추가하여 @Async를 활성화합니다.
@EnableScheduling // <-- 이 어노테이션을 추가하여 스케줄링을 활성화합니다.
public class SimplechatApplication {
	public static void main(String[] args) {
		SpringApplication.run(SimplechatApplication.class, args);
	}
}

@Configuration
class AppConfig {

    @Bean
    @Qualifier("profileFileRepository")
    public FileRepository profileFileRepository(@Value("${file.profile-upload-dir}") String uploadDir) {
        return new FileRepository(uploadDir);
    }

    @Bean
    @Qualifier("chatFileRepository")
    public FileRepository chatFileRepository(@Value("${file.chat-upload-dir}") String uploadDir) {
        return new FileRepository(uploadDir);
    }
}
