package com.example.simplechat.repository;

import org.springframework.stereotype.Repository;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.sql.JDBC_SQL;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RoomRepository {
	public Optional<ChatRoom> findByName(String Name) {
		String sql = "SELECT * FROM chat_rooms WHERE room_name = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{Name});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> roomRow = parsedTable.get(0);
		return Optional.of(mapRowToRoom(roomRow));
	}
	
	public Optional<ChatRoom> findById(Long Id) {
		String sql = "SELECT * FROM chat_rooms WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{String.valueOf(Id)});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> roomRow = parsedTable.get(0);
		return Optional.of(mapRowToRoom(roomRow));
	}
	
	public List<ChatRoom> findByRoomType(ChatRoom.RoomType RoomType) {
		String sql = "SELECT * FROM chat_rooms WHERE room_type = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{RoomType.name()}); // 파라미터 없음

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		// 각 Map을 User 객체로 변환하여 리스트로 만듭니다.
		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	public List<ChatRoom> findByOwnerId(Long ownerId) {
		String sql = "SELECT * FROM chat_rooms WHERE owner_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{String.valueOf(ownerId)}); // 파라미터 없음

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		// 각 Map을 User 객체로 변환하여 리스트로 만듭니다.
		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	private ChatRoom mapRowToRoom(Map<String, Object> row) {
		ChatRoom room = new ChatRoom((Long) row.get("room_id"), (String) row.get("room_name"));
		room.setRoom_type(ChatRoom.RoomType.valueOf((String)row.get("room_type")));
		room.setOwner((Long) row.get("owner_id"));
		room.setCreated_at(((Timestamp)row.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		
		return room;
	}
	
	//////////////////////////////////////////////////////
	// List<Long>을 리턴하고 한 번 더 쿼리를 요청해서 유저정보를 받아올지
	// 지금처럼 한번에 유저정보를 받아서 User 객체를 리턴할지 고민함.
	// 그리고, 방 안에 있는 유저라는 개념이 방과 강하게 결합된 정보 단위라고 생각해서 이렇게 진행함.
	public List<ChatRoomUserDto> findUsersByRoomId(Long roomId){
		String sql = "SELECT u.user_id, cru.nickname, cru.role "+
					"FROM users u INNER JOIN chat_room_users ON u.user_id = cru.user_id "+
					"WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{String.valueOf(roomId)});
		
		// 각 Map을 User 객체로 변환하여 리스트로 만듭니다.
		return parsedTable.stream()
			.map(row -> new ChatRoomUserDto(
					(Long) row.get("user_id"),
					(String) row.get("nickname"),
					(String) row.get("role") ))
			.collect(Collectors.toList());
	}
	
	public ChatRoom save(ChatRoom room) {
		if( room.getId() == null ) {
			return insert(room);
		} else
			return update(room);
	}
	
	private ChatRoom insert(ChatRoom room) {
		// db에 insert하고 id를 받아와 객체에 채움
		String sql = "INSERT INTO chat_rooms (room_name, room_type, owner_id, password_hash) VALUES ( ?, ?, ?, ? )";
		Map<String, Object> result = JDBC_SQL.executeUpdate(sql,
				new String[]{room.getName(), room.getRoom_type().name(), String.valueOf(room.getOwner()), room.getPassword_hash()}, 
				new String[]{"room_id"}, new String[] {"created_at"});
		
		if( result != null ) { 
			room.setId( ((Number)result.get("room_id")).longValue()  ); 
			room.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return room;
	}
	
	private ChatRoom update(ChatRoom room) {
		// db에서 기존값을 가져와서 비교
		Optional<ChatRoom> fromdb = findByName(room.getName());
		if (fromdb.isEmpty())
			throw new RuntimeException("User not found with username: " + room.getName());
		Map<String, Object> Changed = room.getChangedFields(fromdb.get());
		if (Changed.isEmpty()) { return room; } // 변경 사항 없으면 바로 리턴
		
		String sql = "UPDATE chat_rooms SET ";
		String[] values = new String[Changed.size()+1];
		int i = 0;
		for( String key : Changed.keySet() ) {
			sql += key + " = ? , ";
			values[i++] = (String)Changed.get(key);
		}
		sql = sql.substring(0, -2);
		sql += "WHERE id = ?";
		values[i] = ""+room.getId();
		
		// 변경된 필드만 갱신
		Long affectedRows = (long)JDBC_SQL.executeUpdate(sql, values, null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ChatRoom with ID " + room.getId() + " not found or could not be deleted.");
		}
		
		return room;
	}
	
	public void deleteById(Long Id) {
		String sql = "DELETE FROM chat_rooms WHERE room_id = ?";
		Long affectedRows = (long)JDBC_SQL.executeUpdate(sql, new String[]{""+Id}, null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ChatRoom with ID " + Id + " not found or could not be deleted.");
		}
	}
	
	public boolean existsByName(String name) {
		// 전체 컬럼을 가져올 필요 없이, 존재 여부만 확인하면 되므로 COUNT(1)이 효율적입니다.
		String sql = "SELECT COUNT(1) FROM chat_rooms WHERE room_name = ?";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, new String[]{name});

		if (parsedTable.isEmpty()) {
			return false;
		}

		// COUNT 결과는 보통 Long 타입으로 반환됩니다.
		// DB Utils나 JDBC 드라이버에 따라 키 이름이 다를 수 있습니다. (예: "COUNT(1)")
		// 첫 번째 행의 첫 번째 값을 가져옵니다.
		long count = (long) parsedTable.get(0).values().iterator().next();
		return count > 0;
	}
	
	public List<ChatRoom> findAll() {
		String sql = "SELECT * FROM chat_rooms";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, null); // 파라미터 없음

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		// 각 Map을 User 객체로 변환하여 리스트로 만듭니다.
		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	public long count() {
		String sql = "SELECT COUNT(*) FROM chat_rooms";
		List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return 0L;
		}
		
		// existsByUsername과 마찬가지로, 첫 번째 행의 첫 번째 값을 Long으로 변환합니다.
		return (long) parsedTable.get(0).values().iterator().next();
	}
}
