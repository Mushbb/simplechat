package com.example.simplechat.event;

import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserExitedRoomEvent extends ApplicationEvent {
	private final String userId;
	private final String userName;
	private final String roomName;
	
	public UserExitedRoomEvent(Object source, String userId, String userName, String roomName) {
		super(source);
		this.userId = userId;
		this.userName = userName;
		this.roomName = roomName;
	}
	
	public String getUserId() { return userId; }
    public String getRoomName() { return roomName;  }
    public String getUserName() { return userName; }
    
    @Override
    public String toString() {
        return "UserExitedRoomEvent{" +
               "userId=" + userId +
               ", roomName='" + roomName + '\'' +
               '}';
    }    
}