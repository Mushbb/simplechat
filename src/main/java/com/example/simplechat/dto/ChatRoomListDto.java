package com.example.simplechat.dto;

/**
 * 클라이언트에게 채팅방 목록을 보여줄 때 각 채팅방의 요약 정보를 전달하는 DTO입니다.
 *
 * @param id 채팅방의 고유 ID
 * @param name 채팅방의 이름
 * @param roomType 채팅방의 유형 ("PUBLIC", "PRIVATE")
 * @param ownerName 채팅방 개설자의 이름
 * @param userCount 채팅방에 참여한 총 사용자 수
 * @param connCount 현재 채팅방에 접속 중인 사용자 수
 * @param isMember 현재 요청을 보낸 사용자가 이 방의 멤버인지 여부
 */
public record ChatRoomListDto(
    Long id,
    String name,
    String roomType,
    String ownerName,
    Integer userCount,
    Integer connCount,
    boolean isMember
) {

}
