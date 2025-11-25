package com.example.simplechat.config;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SameSite 쿠키 정책을 처리하기 위한 설정 클래스입니다.
 * <p>
 * 최신 브라우저의 SameSite 정책 변경으로 인해 발생할 수 있는 쿠키 전송 문제를 해결합니다.
 * 예를 들어, 다른 도메인의 프론트엔드에서 백엔드 API로 요청을 보낼 때 세션 쿠키가 전송되지 않는 문제를 방지합니다.
 * </p>
 */
@Configuration
public class SameSiteCookieConfig {

    /**
     * Tomcat의 쿠키 프로세서를 커스터마이징하여 SameSite 속성을 'None'으로 설정하는 Bean을 생성합니다.
     * <p>
     * SameSite=None 설정을 사용하려면 Secure 속성(HTTPS)이 필요합니다.
     * 현재는 이 설정이 필요하지 않아 주석 처리되어 있습니다.
     * </p>
     *
     * @return Tomcat 컨텍스트를 커스터마이징하는 {@link TomcatContextCustomizer}
     */
    //@Bean
    public TomcatContextCustomizer sameSiteCookiesConfig() {
        return context -> {
            final Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
            // SameSite 속성을 'None'으로 설정
            cookieProcessor.setSameSiteCookies("None");
            context.setCookieProcessor(cookieProcessor);
        };
    }
}