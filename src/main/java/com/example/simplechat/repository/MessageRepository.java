package com.example.simplechat.repository;

import com.example.simplechat.model.ChatMessage;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 채팅 메시지(ChatMessage) 엔티티의 영속성(데이터베이스 CRUD)을 관리하는 리포지토리 클래스입니다.
 * {@link JDBC_SQL}을 사용하여 데이터베이스와 상호작용합니다.
 */
@RequiredArgsConstructor
@Repository
public class MessageRepository {
	private final JDBC_SQL jdbcsql;
	
	/**
	 * 특정 채팅방의 모든 메시지를 조회합니다.
	 *
	 * @param roomId 메시지를 조회할 방의 ID
	 * @return 해당 방의 {@link ChatMessage} 객체 목록. 메시지가 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatMessage> findByRoomId(Long roomId){
		String sql = "SELECT * FROM chat_messages WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{roomId});
		
		if( parsedTable.isEmpty() )
			return Collections.emptyList();

		return parsedTable.stream()
				.map(this::mapRowToMsg)
				.collect(Collectors.toList());
	}
	
	/**
	 * 특정 채팅방에서 지정된 개수(N)만큼의 메시지를 ID 기준으로 정렬하여 조회합니다.
	 * `beginId`를 기준으로 이전 또는 이후 메시지를 조회할 수 있습니다.
	 *
	 * @param roomId 메시지를 조회할 방의 ID
	 * @param beginId (선택 사항) 조회 시작 기준이 되는 메시지 ID. null인 경우 최신 메시지부터 조회.
	 * @param N 조회할 메시지의 최대 개수
	 * @param Sort 정렬 순서 ("ASC" 또는 "DESC")
	 * @return 조회된 {@link ChatMessage} 객체 목록. 메시지가 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatMessage> findTopNByRoomIdOrderById(Long roomId, Long beginId, Integer N, String Sort) {
		StringBuilder sql = new StringBuilder("SELECT TOP ").append(N).append(" * FROM chat_messages WHERE room_id = ?");
		List<Object> params = new ArrayList<>();
		
		params.add(roomId);
		if (beginId != null) {
			sql.append(" AND message_id < ?"); // DESC 정렬을 가정 (더 작은 ID = 더 오래된 메시지)
			params.add(beginId);
		}
		sql.append(" ORDER BY message_id ").append(Sort);
		
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql.toString(), params.toArray());
	
		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
			.map(this::mapRowToMsg)
			.collect(Collectors.toList());
	}
	
	/**
	 * 특정 채팅방에서 특정 작성자가 보낸 모든 메시지를 조회합니다.
	 *
	 * @param roomId 메시지를 조회할 방의 ID
	 * @param authorId 메시지를 보낸 작성자의 ID
	 * @return 해당 방과 작성자의 {@link ChatMessage} 객체 목록. 메시지가 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatMessage> findByRoomIdandAuthorId(Long roomId, Long authorId){
		String sql = "SELECT * FROM chat_messages WHERE room_id = ? AND author_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{roomId, authorId});
		
		if( parsedTable.isEmpty() )
			return Collections.emptyList();
		
		return parsedTable.stream()
				.map(this::mapRowToMsg)
				.collect(Collectors.toList());
	}
	
	/**
	 * 채팅 메시지를 저장합니다. 메시지 ID가 없으면 새로운 메시지를 삽입하고, ID가 있으면 기존 메시지를 업데이트합니다.
	 *
	 * @param msg 저장할 {@link ChatMessage} 객체
	 * @return 저장 또는 업데이트된 {@link ChatMessage} 객체 (새 ID 및 생성 시간 포함)
	 */
	public ChatMessage save(ChatMessage msg) {
		if( msg.getId() == null )
			return insert(msg);
		else
			return update(msg);
	}
	
	/**
	 * 새로운 채팅 메시지를 데이터베이스에 삽입합니다.
	 * 생성된 메시지 ID와 생성 시간을 {@link ChatMessage} 객체에 채웁니다.
	 *
	 * @param msg 삽입할 {@link ChatMessage} 객체
	 * @return ID와 생성 시간이 채워진 {@link ChatMessage} 객체
	 */
	private ChatMessage insert(ChatMessage msg) {
		StringBuilder sql = new StringBuilder("INSERT INTO chat_messages (room_id, author_id, author_name, message_type, content");
		List<Object> params = new ArrayList<>(Arrays.asList(msg.getRoom_id(), msg.getAuthor_id(), msg.getAuthor_name(), msg.getMsg_type().name(), msg.getContent()));

		if( msg.getParent_msg_id() != null ) {
			sql.append(", parent_message_id");
			params.add(msg.getParent_msg_id());
		}
		sql.append(") VALUES (?, ?, ?, ?, ?");
		if( msg.getParent_msg_id() != null ) {
			sql.append(", ?");
		}
		sql.append(")");
		
		Map<String, Object> result = jdbcsql.executeUpdate(sql.toString(), params.toArray(), 
				new String[]{"message_id"}, new String[] {"created_at"});
		
		if( result != null && result.containsKey("message_id")) { 
			msg.setId( ((Number)result.get("message_id")).longValue() ); 
			msg.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return msg;
	}
	
	/**
	 * 기존 채팅 메시지의 내용을 업데이트합니다.
	 *
	 * @param msg 업데이트할 {@link ChatMessage} 객체 (ID와 새 내용 포함)
	 * @return 업데이트된 {@link ChatMessage} 객체
	 */
	private ChatMessage update(ChatMessage msg) {
		String sql = "UPDATE chat_messages SET content = ?, message_type = ? WHERE message_id = ?";
		// msg.getMsg_type().name() 추가하여 메시지 타입도 업데이트 되도록 수정. UPDATE 타입인 경우.
		jdbcsql.executeUpdate(sql, new Object[]{msg.getContent(), msg.getMsg_type().name(), msg.getId()}, null, null);
		return msg;
	}
	
	/**
	 * 데이터베이스 조회 결과의 Map 형태 한 행을 {@link ChatMessage} 엔티티로 매핑합니다.
	 *
	 * @param row 데이터베이스에서 조회된 단일 행을 나타내는 Map
	 * @return 매핑된 {@link ChatMessage} 객체
	 */
	private ChatMessage mapRowToMsg(Map<String, Object> row) {
		ChatMessage msg = new ChatMessage((Long) row.get("message_id"), (Long) row.get("author_id"), (String) row.get("author_name"), (Long) row.get("room_id"));
		msg.setMsg_type(ChatMessage.MsgType.valueOf((String)row.get("message_type")));
		msg.setContent((String) row.get("content"));
		// Timestamp 객체가 아닐 수 있으므로 toString()을 통해 안전하게 처리
		Object createdAtObj = row.get("created_at");
		if (createdAtObj instanceof Timestamp) {
			msg.setCreated_at(((Timestamp) createdAtObj).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		} else if (createdAtObj != null) {
			msg.setCreated_at(createdAtObj.toString());
		} else {
			msg.setCreated_at(null);
		}
		
		// parent_message_id가 없을 수도 있으므로 확인 후 설정
		Object parentMsgIdObj = row.get("parent_message_id");
		if (parentMsgIdObj instanceof Number) {
		    msg.setParent_msg_id(((Number) parentMsgIdObj).longValue());
		} else {
		    msg.setParent_msg_id(null);
		}
		
		return msg;
	}

	/**
	 * 메시지 ID를 기준으로 단일 채팅 메시지를 조회합니다.
	 *
	 * @param messageId 조회할 메시지의 ID
	 * @return 메시지를 포함하는 {@link Optional<ChatMessage>} 객체. 메시지가 없으면 Optional.empty() 반환.
	 */
	public Optional<ChatMessage> findById(Long messageId) {
		String sql = "SELECT * FROM chat_messages WHERE message_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{messageId});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		return Optional.of(mapRowToMsg(parsedTable.get(0)));
	}

	/**
	 * 메시지 ID를 기준으로 채팅 메시지를 삭제합니다.
	 *
	 * @param messageId 삭제할 메시지의 ID
	 */
	public void deleteById(Long messageId) {
		String sql = "DELETE FROM chat_messages WHERE message_id = ?";
		jdbcsql.executeUpdate(sql, new Object[]{messageId}, null, null);
	}

	/**
	 * 특정 채팅방의 모든 메시지를 삭제합니다.
	 *
	 * @param roomId 메시지를 삭제할 방의 ID
	 */
	public void deleteByRoomId(Long roomId) {
		String sql = "DELETE FROM chat_messages WHERE room_id = ?";
		jdbcsql.executeUpdate(sql, new Object[]{roomId}, null, null);
	}
}
