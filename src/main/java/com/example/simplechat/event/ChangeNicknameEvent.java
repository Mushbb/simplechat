package com.example.simplechat.event;

import org.springframework.context.ApplicationEvent;

/**
 * 사용자가 채팅방 내에서 닉네임을 변경했을 때 발행되는 애플리케이션 이벤트입니다.
 * 이 이벤트를 통해 다른 컴포넌트들이 닉네임 변경 사실을 인지하고 필요한 후속 조치를 취할 수 있습니다.
 */
public class ChangeNicknameEvent extends ApplicationEvent {
	private final Long userId;
	private final Long roomId;
	private final String newNickname;
	
	/**
	 * 새로운 ChangeNicknameEvent를 생성합니다.
	 * @param source 이벤트의 원본 객체 (보통 this)
	 * @param userId 닉네임을 변경한 사용자의 ID
	 * @param roomId 닉네임이 변경된 채팅방의 ID
	 * @param newNickname 새로 변경된 닉네임
	 */
	public ChangeNicknameEvent(Object source, Long userId, Long roomId, String newNickname) {
		super(source);
		this.userId = userId;
		this.roomId = roomId;
		this.newNickname = newNickname;
	}
	
	/**
	 * 닉네임을 변경한 사용자의 ID를 반환합니다.
	 * @return 사용자의 ID
	 */
	public Long getUserId() {
		return userId;
	}
	
    /**
     * 닉네임이 변경된 채팅방의 ID를 반환합니다.
     * @return 채팅방의 ID
     */
    public Long getRoomId() {
        return roomId;
    }
    
    /**
     * 새로 변경된 닉네임을 반환합니다.
     * @return 새로운 닉네임
     */
    public String getNewNickname() {
    	return newNickname;
    }
    
    /**
     * 이벤트 정보를 문자열 형태로 반환합니다.
     * @return 이벤트 정보 문자열
     */
    @Override
    public String toString() {
        return "ChangeNicknameEvent{" +
               "UserInfo=" + userId +
               ", roomName='" + roomId + '\'' + ", nickName='" + newNickname + '\'' +
               '}';
    }    
}