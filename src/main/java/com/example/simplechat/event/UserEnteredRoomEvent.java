package com.example.simplechat.event;

import com.example.simplechat.model.UserInfo;
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserEnteredRoomEvent extends ApplicationEvent {
	private final UserInfo userinfo;
	private final String roomName;
	
	public UserEnteredRoomEvent(Object source, UserInfo userinfo, String roomName) {
		super(source);
		this.userinfo = userinfo;
		this.roomName = roomName;
	}
	
	
	public UserInfo getUserInfo() {
		return userinfo;
	}
	
    public String getRoomName() {
        return roomName;
    }
    
    @Override
    public String toString() {
        return "UserEnteredRoomEvent{" +
               "UserInfo=" + userinfo +
               ", roomName='" + roomName + '\'' +
               '}';
    }    
}