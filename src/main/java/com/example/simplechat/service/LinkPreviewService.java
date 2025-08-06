package com.example.simplechat.service;

import com.example.simplechat.dto.LinkPreviewDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LinkPreviewService {

    private final SimpMessagingTemplate messagingTemplate;

    public LinkPreviewService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://(www\\.)?[-a-zA-Z0-9@:%.\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*))"
    );

    public String findFirstUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    @Async
    public void generateAndSendPreview(Long messageId, Long roomId, String urlString) {
        if (isYoutubeUrl(urlString)) {
            return; // 유튜브 URL은 미리보기를 생성하지 않음
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
            System.err.println("Error generating link preview for " + urlString + ": " + e.getMessage());
        }
    }

    private String getMetaTagContent(Document doc, String property) {
        return doc.select("meta[property=" + property + "]").attr("content");
    }

    private boolean isYoutubeUrl(String url) {
        if (url == null) return false;
        String lowerCaseUrl = url.toLowerCase();
        return lowerCaseUrl.contains("youtube.com") || lowerCaseUrl.contains("youtu.be");
    }
}
