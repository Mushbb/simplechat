package com.example.simplechat.event;

import com.example.simplechat.model.User;
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserEnteredRoomEvent extends ApplicationEvent {
	private final User user;
	private final Long roomId;
	
	public UserEnteredRoomEvent(Object source, User userinfo, Long roomid) {
		super(source);
		this.user = userinfo;
		this.roomId = roomid;
	}
	
	
	public User getUser() {
		return user;
	}
	
    public Long getRoomId() {
        return roomId;
    }
    
    @Override
    public String toString() {
        return "UserEnteredRoomEvent{" +
               "UserInfo=" + user +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}