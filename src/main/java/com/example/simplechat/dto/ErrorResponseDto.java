package com.example.simplechat.dto;

/**
 * 클라이언트에 전송되는 에러 응답을 표준화하기 위한 DTO입니다.
 *
 * @param errorCode 에러를 식별하는 코드 (예: "NOT_FOUND", "UNAUTHORIZED")
 * @param message 사용자에게 표시할 에러 메시지
 */
public record ErrorResponseDto(
    String errorCode,
    String message
) {

}
