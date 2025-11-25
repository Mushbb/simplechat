package com.example.simplechat.service;

import com.example.simplechat.dto.LinkPreviewDto;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 채팅 메시지에 포함된 URL에 대한 링크 미리보기를 생성하고 전송하는 서비스입니다.
 * Jsoup 라이브러리를 사용하여 웹 페이지의 메타 정보를 추출하고, WebSocket을 통해 클라이언트에 미리보기를 보냅니다.
 */
@Service
public class LinkPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(LinkPreviewService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public LinkPreviewService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://(www\\.)?[-a-zA-Z0-9@:%.\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*))"
    );

    /**
     * 주어진 텍스트에서 첫 번째 URL을 찾아 반환합니다.
     *
     * @param text URL을 찾을 문자열
     * @return 발견된 첫 번째 URL 문자열, 없으면 null
     */
    public String findFirstUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    /**
     * URL에 대한 링크 미리보기를 비동기적으로 생성하고, WebSocket을 통해 해당 방의 클라이언트에 전송합니다.
     * 유튜브 URL이나 직접 미디어 링크는 미리보기를 생성하지 않습니다.
     *
     * @param messageId 미리보기 정보를 연관시킬 메시지의 ID
     * @param roomId 미리보기 정보를 전송할 채팅방의 ID
     * @param urlString 미리보기를 생성할 URL
     */
    @Async
    public void generateAndSendPreview(Long messageId, Long roomId, String urlString) {
        if (isYoutubeUrl(urlString) || isDirectMediaLink(urlString)) {
            return;
        }

        try {
            Document doc = Jsoup.connect(urlString)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                                .timeout(5000) // 5초 타임아웃
                                .get();

            String title = getMetaTagContent(doc, "og:title");
            if (title == null || title.isEmpty()) {
                title = doc.title();
            }

            String description = getMetaTagContent(doc, "og:description");

            String imageUrl = getMetaTagContent(doc, "og:image");
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = new URI(urlString).resolve(imageUrl).toString();
            }

            LinkPreviewDto previewDto = new LinkPreviewDto(messageId, urlString, title, description, imageUrl);

            messagingTemplate.convertAndSend("/topic/" + roomId + "/previews", previewDto);

        } catch (IOException | URISyntaxException e) {
            logger.error("URL {}에 대한 링크 미리보기 생성 중 오류 발생: {}", urlString, e.getMessage(), e);
        }
    }

    /**
     * HTML 문서에서 특정 Open Graph 메타 태그의 내용을 추출합니다.
     *
     * @param doc Jsoup으로 파싱된 HTML 문서
     * @param property 추출할 메타 태그의 'property' 속성 값 (예: "og:title")
     * @return 메타 태그의 'content' 속성 값, 없으면 빈 문자열
     */
    private String getMetaTagContent(Document doc, String property) {
        return doc.select("meta[property=" + property + "]").attr("content");
    }

    /**
     * 주어진 URL이 YouTube 링크인지 확인합니다.
     *
     * @param url 확인할 URL 문자열
     * @return YouTube 링크이면 true, 그렇지 않으면 false
     */
    private boolean isYoutubeUrl(String url) {
        if (url == null) return false;
        String lowerCaseUrl = url.toLowerCase();
        return lowerCaseUrl.contains("youtube.com") || lowerCaseUrl.contains("youtu.be");
    }
    
    /**
     * 주어진 URL이 이미지 또는 비디오와 같은 직접 미디어 파일 링크인지 확인합니다.
     *
     * @param url 확인할 URL 문자열
     * @return 직접 미디어 파일 링크이면 true, 그렇지 않으면 false
     */
    private boolean isDirectMediaLink(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String path = url.split("\\?")[0].toLowerCase();
        
        return path.endsWith(".jpg") ||
               path.endsWith(".jpeg") ||
               path.endsWith(".png") ||
               path.endsWith(".gif") ||
               path.endsWith(".webp") ||
               path.endsWith(".mp4");
    }

}
