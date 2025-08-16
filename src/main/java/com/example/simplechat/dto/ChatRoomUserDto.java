package com.example.simplechat.dto;

public record ChatRoomUserDto(
	Long userId, 
	String nickname, 
	UserType role,
	ConnectType conn,
	String profileImageUrl
) {
	public enum UserType {
    	ADMIN,
    	MEMBER
    }
	
	public enum ConnectType {
		CONNECT,
		DISCONNECT
	}
}