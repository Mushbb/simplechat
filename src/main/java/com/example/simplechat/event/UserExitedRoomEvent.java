package com.example.simplechat.event;

import com.example.simplechat.dto.UserEventDto.EventType;
import org.springframework.context.ApplicationEvent;

/**
 * 사용자가 채팅방에서 퇴장하거나 (강제 퇴장 포함), 방이 삭제될 때 발행되는 애플리케이션 이벤트입니다.
 * 이 이벤트를 통해 관련 컴포넌트들이 사용자 상태 변화나 방의 삭제를 인지하고 필요한 후속 조치를 취할 수 있습니다.
 */
public class UserExitedRoomEvent extends ApplicationEvent {
	private final Long userId;
	private final Long roomId;
	private final EventType eventType;
	
	/**
	 * 새로운 UserExitedRoomEvent를 생성합니다.
	 * @param source 이벤트의 원본 객체 (보통 this)
	 * @param userId 퇴장한 사용자의 ID (방 삭제 이벤트의 경우 null일 수 있음)
	 * @param roomId 이벤트가 발생한 채팅방의 ID
	 * @param eventType 발생한 이벤트의 유형 (예: ROOM_OUT, ROOM_DELETED)
	 */
	public UserExitedRoomEvent(Object source, Long userId, Long roomId, EventType eventType) {
		super(source);
		this.userId = userId;
		this.roomId = roomId;
		this.eventType = eventType;
	}
	
	/**
	 * 이벤트와 관련된 사용자의 ID를 반환합니다.
	 * @return 사용자의 ID
	 */
	public Long getUserId() { return userId; }
	
    /**
     * 이벤트가 발생한 채팅방의 ID를 반환합니다.
     * @return 채팅방의 ID
     */
    public Long getRoomId() { return roomId;  }
    
    /**
     * 발생한 이벤트의 유형을 반환합니다.
     * @return 이벤트 유형
     */
    public EventType getEventType() { return eventType; }
    
    /**
     * 이벤트 정보를 문자열 형태로 반환합니다.
     * @return 이벤트 정보 문자열
     */
    @Override
    public String toString() {
        return "UserExitedRoomEvent{" +
               "userId=" + userId +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}