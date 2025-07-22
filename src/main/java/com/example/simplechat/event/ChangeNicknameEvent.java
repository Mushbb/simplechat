package com.example.simplechat.event;

import com.example.simplechat.model.User;
import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class ChangeNicknameEvent extends ApplicationEvent {
	private final User userinfo;
	private final String roomName;
	
	public ChangeNicknameEvent(Object source, User userinfo, String roomName) {
		super(source);
		this.userinfo = userinfo;
		this.roomName = roomName;
	}
	
	
	public User getUserInfo() {
		return userinfo;
	}
	
    public String getRoomName() {
        return roomName;
    }
    
    @Override
    public String toString() {
        return "ChangeNicknameEvent{" +
               "UserInfo=" + userinfo +
               ", roomName='" + roomName + '\'' + ", nickName='" + userinfo.getUsername() + '\'' +
               '}';
    }    
}