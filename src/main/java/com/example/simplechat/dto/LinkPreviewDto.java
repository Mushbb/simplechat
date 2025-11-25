package com.example.simplechat.dto;

/**
 * 채팅 메시지에 포함된 URL에 대한 링크 미리보기 정보를 클라이언트에 전송하기 위한 DTO입니다.
 *
 * @param messageId 미리보기 정보와 연관된 메시지의 ID
 * @param url 미리보기가 생성된 원본 URL
 * @param title 링크 미리보기의 제목
 * @param description 링크 미리보기의 설명
 * @param imageUrl 링크 미리보기의 대표 이미지 URL
 */
public record LinkPreviewDto(
    Long messageId,
    String url,
    String title,
    String description,
    String imageUrl
) {

}
