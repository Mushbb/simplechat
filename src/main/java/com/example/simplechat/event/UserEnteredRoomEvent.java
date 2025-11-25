package com.example.simplechat.event;

import com.example.simplechat.model.User;
import com.example.simplechat.dto.UserEventDto.UserType;
import org.springframework.context.ApplicationEvent;

/**
 * 사용자가 채팅방에 성공적으로 입장했을 때 발행되는 애플리케이션 이벤트입니다.
 * 이 이벤트를 통해 다른 컴포넌트들이 사용자 입장 사실을 인지하고 필요한 후속 조치를 취할 수 있습니다.
 */
public class UserEnteredRoomEvent extends ApplicationEvent {
	private final User user;
	private final Long roomId;
	private final UserType userType;
	
	/**
	 * 새로운 UserEnteredRoomEvent를 생성합니다.
	 * @param source 이벤트의 원본 객체 (보통 this)
	 * @param userInfo 채팅방에 입장한 사용자 정보
	 * @param roomid 사용자가 입장한 채팅방의 ID
	 * @param userType 사용자의 역할 (ADMIN, MEMBER 등)
	 */
	public UserEnteredRoomEvent(Object source, User userInfo, Long roomid, UserType userType) {
		super(source);
		this.user = userInfo;
		this.roomId = roomid;
		this.userType = userType;
	}
	
	/**
	 * 채팅방에 입장한 사용자의 정보를 반환합니다.
	 * @return 사용자 객체
	 */
	public User getUser() {
		return user;
	}
	
    /**
     * 사용자가 입장한 채팅방의 ID를 반환합니다.
     * @return 채팅방의 ID
     */
    public Long getRoomId() {
        return roomId;
    }
    
    /**
     * 사용자의 역할을 반환합니다.
     * @return 사용자의 역할 유형
     */
    public UserType getUserType() {
    	return userType;
    }
    
    /**
     * 이벤트 정보를 문자열 형태로 반환합니다.
     * @return 이벤트 정보 문자열
     */
    @Override
    public String toString() {
        return "UserEnteredRoomEvent{" +
               "UserInfo=" + user +
               ", roomId='" + roomId + '\'' +
               '}';
    }    
}