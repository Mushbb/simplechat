package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.example.simplechat.dto.ChatMessageRequestDto;

@Setter
@Getter
public class ChatMessage {
	public ChatMessage(Long id, Long room_id, MsgType messageType) {
		this.id = id;
		this.room_id = room_id;
		this.msg_type = messageType;
	}

	public enum MsgType {
		TEXT,
		IMAGE,
		VIDEO,
		FILE,
		DELETE,
		UPDATE
	}
	
	private Long id;
	private Long room_id;
	private Long author_id;
	private String author_name;
	private String content;
	private String created_at;
	private MsgType msg_type;
	private Long parent_msg_id;
	
	public ChatMessage(){ }
	public ChatMessage(Long author_id, Long room_id, String author_name) { this.author_id = author_id; this.room_id = room_id; this.author_name = author_name;}
	public ChatMessage(Long newId, Long author_id, String author_name, Long room_id){ 
		this.id = newId; 
		this.author_id = author_id; 
		this.author_name = author_name; 
		this.room_id = room_id; 
	}
	public ChatMessage(Long room_id, Long author_id, String author_name, String content, MsgType msg_type) {
		this.room_id = room_id;
		this.author_id = author_id;
		this.author_name = author_name;
		this.content = content;
		this.msg_type = msg_type;
	}
	public ChatMessage(ChatMessageRequestDto dto, String AuthorName) {
	    this.room_id = dto.roomId();
	    this.author_id = dto.authorId();
	    this.author_name = AuthorName;
	    this.content = dto.content();
	    this.msg_type = dto.messageType();
	}
	
    public Map<String, Object> getChangedFields(ChatMessage oldMsg) {
	    Map<String, Object> changes = new HashMap<>();

	    // 메시지 비교
	    if (!Objects.equals(this.content, oldMsg.content))
	        changes.put("nickname", this.content);
	    
	    return changes;
	}
}