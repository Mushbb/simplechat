package com.example.simplechat.exception;

import lombok.Getter;

/**
 * 사용자 등록 및 관련 인증/권한 부여 과정에서 발생하는 특정 오류를 나타내는 사용자 정의 예외입니다.
 * 이 예외는 특정 오류 코드를 포함하여 클라이언트가 오류의 원인을 더 명확하게 이해하고 처리할 수 있도록 돕습니다.
 */
@Getter
public class RegistrationException extends RuntimeException {
    /**
     * 오류를 식별하는 코드입니다 (예: "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "CONFLICT").
     */
    private final String errorCode;

    /**
     * 새로운 RegistrationException을 생성합니다.
     * @param errorCode 오류를 식별하는 코드
     * @param message 사용자에게 표시할 오류 메시지
     */
    public RegistrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
