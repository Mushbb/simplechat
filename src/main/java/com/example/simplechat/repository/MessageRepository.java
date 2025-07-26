package com.example.simplechat.repository;

import org.springframework.stereotype.Repository;
import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.dto.ChatMessageDto;
import com.example.sql.JDBC_SQL;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MessageRepository {
	public List<ChatMessage> findByRoomId(Long roomId){
		String sql = "SELECT * FROM chat_messages WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{String.valueOf(roomId)});
		
		if( parsedTable.isEmpty() )
			return Collections.emptyList();

		return parsedTable.stream()
				.map(this::mapRowToMsg)
				.collect(Collectors.toList());
	}
	
	public List<ChatMessage> findByRoomIdandAuthorId(Long roomId, Long authorId){
		String sql = "SELECT * FROM chat_messages WHERE room_id = ? AND author_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{String.valueOf(roomId), String.valueOf(authorId)});
		
		if( parsedTable.isEmpty() )
			return Collections.emptyList();
		
		return parsedTable.stream()
				.map(this::mapRowToMsg)
				.collect(Collectors.toList());
	}
	
	public ChatMessage save(ChatMessage msg) {
		if( msg.getId() == null )
			return insert(msg);
		else
			return update(msg);
	}
	
	private ChatMessage insert(ChatMessage msg) {
		// db에 insert하고 id를 받아와 객체에 채움
		String sql = "INSERT INTO chat_messages (room_id, author_id, author_name, message_type, content"+
				(msg.getParent_msg_id()!=null?", parent_message_id":"")+") VALUES ( ?, ?, ?, ?, ?"+
				(msg.getParent_msg_id()!=null?", ?":"")+" )";
		List<String> Params = new ArrayList<>(Arrays.asList(""+msg.getRoom_id(), ""+msg.getAuthor_id(), msg.getAuthor_name(), msg.getMsg_type().name(), msg.getContent()));
		if( msg.getParent_msg_id() != null )
			Params.add(""+msg.getParent_msg_id());
		
		Map<String, Object> result = JDBC_SQL.executeUpdate(sql, Params.toArray(new String[0]), 
				new String[]{"message_id"}, new String[] {"created_at"});
		
		if( result != null ) { 
			msg.setId( ((Number)result.get("message_id")).longValue() ); 
			msg.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return msg;
	}
	
	private ChatMessage update(ChatMessage msg) {
		throw new RuntimeException("Not Implemented");
		//return msg;
	}
	
	private ChatMessage mapRowToMsg(Map<String, Object> row) {
		ChatMessage msg = new ChatMessage((Long) row.get("message_id"), (Long) row.get("author_id"), (String) row.get("author_name"), (Long) row.get("room_id"));
		msg.setMsg_type(ChatMessage.MsgType.valueOf((String)row.get("message_type")));
		msg.setContent((String) row.get("content"));
		msg.setCreated_at(((Timestamp)row.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		msg.setParent_msg_id((Long) row.get("parent_message_id"));
		
		return msg;
	}
}
