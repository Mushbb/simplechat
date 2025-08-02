package com.example.simplechat.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class User {
	private String username;
	private Long id;
	private String password_hash;
	private String nickname;
	private String status_message;
	private String profile_image_url;
	private String created_at;
	
	
	public User() { }
	public User( Long newId, String Name ) { username = Name; id = newId; }
	public User( String newId, String Name ) { username = Name; id = Long.getLong(newId); }
	public User( String Name, String Pass, String Nick ) {
		username = Name;
		password_hash = Pass;
		nickname = Nick;
	}
	
	public Map<String, Object> getChangedFields(User oldUser) {
	    Map<String, Object> changes = new HashMap<>();

	    // 닉네임 비교
	    if (!Objects.equals(this.nickname, oldUser.nickname)) {
	        changes.put("nickname", this.nickname);
	    }
	    // 비밀번호 해시는 보통 별도 메서드로 처리하지만, 예시상 포함
	    if (!Objects.equals(this.password_hash, oldUser.password_hash)) {
	        changes.put("password_hash", this.password_hash);
	    }
	    // 상태 메시지 비교
	    if (!Objects.equals(this.status_message, oldUser.status_message)) {
	        changes.put("status_message", this.status_message);
	    }
	    // 프로필 이미지 URL 비교
	    if (!Objects.equals(this.profile_image_url, oldUser.profile_image_url)) {
	        changes.put("profile_image_url", this.profile_image_url);
	    }
	    
	    return changes;
	}
}