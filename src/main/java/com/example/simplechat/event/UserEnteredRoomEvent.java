package com.example.simplechat.event;

import com.example.simplechat.model.User;
import com.example.simplechat.dto.UserEventDto.UserType;
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class UserEnteredRoomEvent extends ApplicationEvent {
	private final User user;
	private final Long roomId;
	private final UserType userType;
	
	public UserEnteredRoomEvent(Object source, User userinfo, Long roomid, UserType userType) {
		super(source);
		this.user = userinfo;
		this.roomId = roomid;
		this.userType = userType;
	}
	
	
	public User getUser() {
		return user;
	}
	
    public Long getRoomId() {
        return roomId;
    }
    
    public UserType getUserType() {
    	return userType;
    }
    
    @Override
    public String toString() {
        return "UserEnteredRoomEvent{" +
               "UserInfo=" + user +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}