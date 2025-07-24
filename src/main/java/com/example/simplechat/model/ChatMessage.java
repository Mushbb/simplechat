package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
public class ChatMessage {
	public static enum MsgType {
		TEXT, IMAGE, FILE, SYSTEM
	}
	
	private Long id;
	private Long author_id;
	private Long room_id;
	private String content;
	private String created_at;
	private MsgType msg_type;
	private Long parent_msg_id;
	
	public ChatMessage(){ }
	public ChatMessage(Long author, Long room) { author_id = author; room_id = room; }
	public ChatMessage(Long newId, Long author, Long room){ id = newId; author_id = author; room_id = room; }
	
    public Map<String, Object> getChangedFields(ChatMessage oldMsg) {
	    Map<String, Object> changes = new HashMap<>();

	    // 메시지 비교
	    if (!Objects.equals(this.content, oldMsg.content))
	        changes.put("nickname", this.content);
	    
	    return changes;
	}
}