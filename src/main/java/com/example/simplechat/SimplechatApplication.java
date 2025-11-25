package com.example.simplechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; 		// 비동기 활성화 어노테이션
import org.springframework.scheduling.annotation.EnableScheduling; 	// 스케줄링 활성화 어노테이션
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.simplechat.repository.FileRepository;


@SpringBootApplication(exclude ={UserDetailsServiceAutoConfiguration.class})
@EnableAsync
@EnableScheduling
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
