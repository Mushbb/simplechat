package com.example.simplechat.event;

import com.example.simplechat.dto.UserEventDto.EventType;
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserExitedRoomEvent extends ApplicationEvent {
	private final Long userId;
	private final Long roomId;
	private final EventType eventType;
	
	public UserExitedRoomEvent(Object source, Long userId, Long roomId, EventType eventType) {
		super(source);
		this.userId = userId;
		this.roomId = roomId;
		this.eventType = eventType;
	}
	
	public Long getUserId() { return userId; }
    public Long getRoomId() { return roomId;  }
    public EventType getEventType() { return eventType; }
    @Override
    public String toString() {
        return "UserExitedRoomEvent{" +
               "userId=" + userId +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}