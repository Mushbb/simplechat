package com.example.simplechat.dto;

/**
 * 클라이언트가 특정 채팅방의 메시지 목록을 요청할 때 사용하는 DTO입니다.
 * 커서 기반 페이지네이션을 위한 정보를 담고 있습니다.
 *
 * @param roomId 메시지 목록을 조회할 방의 ID
 * @param beginId 메시지 조회를 시작할 기준이 되는 메시지 ID (이 ID 이전의 메시지를 조회)
 * @param rowCount 조회할 메시지의 개수
 */
public record ChatMessageListRequestDto(
    Long roomId,
    Long beginId,
    Integer rowCount
) {

}