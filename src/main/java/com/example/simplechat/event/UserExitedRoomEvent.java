package com.example.simplechat.event;

import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserExitedRoomEvent extends ApplicationEvent {
	private final Long userId;
	private final Long roomId;
	
	public UserExitedRoomEvent(Object source, Long userId, Long roomId) {
		super(source);
		this.userId = userId;
		this.roomId = roomId;
	}
	
	public Long getUserId() { return userId; }
    public Long getRoomId() { return roomId;  }
    @Override
    public String toString() {
        return "UserExitedRoomEvent{" +
               "userId=" + userId +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}