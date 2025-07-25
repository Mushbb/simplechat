package com.example.simplechat.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRoom {
	public static enum RoomType {
		PUBLIC, PRIVATE, GAME
	}
	
	private String name;
	private Long id;
	private RoomType room_type;
	private Long owner;
	private String created_at;
	private String password_hash;
	
    private final List<ChatMessage> chats = new CopyOnWriteArrayList<>();
    private final Map<Integer, User> users = new HashMap<>();
    private final User admin = new User("-1", "Server" );
    
    public ChatRoom(String newName) { name = newName; }
    public ChatRoom(Long newId, String newName) { name = newName; id = newId; }
    public ChatRoom(String newName, RoomType roomtype, Long owner_id, String password_hash) {
    	this.name = newName;
    	this.room_type = roomtype;
    	this.owner = owner_id;
    	this.password_hash = password_hash;
    }
    
    public Map<String, Object> getChangedFields(ChatRoom oldRoom) {
	    Map<String, Object> changes = new HashMap<>();

	    // 닉네임 비교
	    if (!Objects.equals(this.name, oldRoom.name))
	        changes.put("nickname", this.name);
	    // 비밀번호 해시는 보통 별도 메서드로 처리하지만, 예시상 포함
	    if (!Objects.equals(this.password_hash, oldRoom.password_hash))
	        changes.put("password_hash", this.password_hash);
	    // room_type
	    if (!Objects.equals(this.room_type, oldRoom.room_type))
	        changes.put("nickname", this.room_type);
	    // owner
	    if (!Objects.equals(this.owner, oldRoom.owner))
	        changes.put("nickname", this.owner);
	    
	    return changes;
	}
}