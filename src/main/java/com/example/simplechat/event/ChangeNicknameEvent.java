package com.example.simplechat.event;

import org.springframework.context.ApplicationEvent; // Spring의 ApplicationEvent 상속

public class ChangeNicknameEvent extends ApplicationEvent {
	private final Long userId;
	private final Long roomId;
	private final String newNickname;
	
	public ChangeNicknameEvent(Object source, Long userId, Long roomId, String newNickname) {
		super(source);
		this.userId = userId;
		this.roomId = roomId;
		this.newNickname = newNickname;
	}
	
	public Long getUserId() {
		return userId;
	}
	
    public Long getRoomId() {
        return roomId;
    }
    
    public String getNewNickname() {
    	return newNickname;
    }
    
    
    @Override
    public String toString() {
        return "ChangeNicknameEvent{" +
               "UserInfo=" + userId +
               ", roomName='" + roomId + '\'' + ", nickName='" + newNickname + '\'' +
               '}';
    }    
}