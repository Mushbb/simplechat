package com.example.simplechat.repository;

import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.service.RoomSessionManager;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * 채팅방(ChatRoom) 엔티티의 영속성(데이터베이스 CRUD)을 관리하는 리포지토리 클래스입니다.
 * {@link JDBC_SQL}을 사용하여 데이터베이스와 상호작용합니다.
 */
@RequiredArgsConstructor
@Repository
public class RoomRepository {
	private final RoomSessionManager roomSessionManager;
	private final JDBC_SQL jdbcsql;
    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;
	
	/**
	 * 채팅방 이름으로 채팅방을 조회합니다.
	 *
	 * @param name 조회할 채팅방의 이름
	 * @return 해당 이름의 {@link ChatRoom} 객체를 포함하는 {@link Optional}. 방이 없으면 Optional.empty() 반환.
	 */
	public Optional<ChatRoom> findByName(String name) {
		String sql = "SELECT * FROM chat_rooms WHERE room_name = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{name});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> roomRow = parsedTable.get(0);
		return Optional.of(mapRowToRoom(roomRow));
	}
	
	/**
	 * 채팅방 ID로 채팅방을 조회합니다.
	 *
	 * @param id 조회할 채팅방의 ID
	 * @return 해당 ID의 {@link ChatRoom} 객체를 포함하는 {@link Optional}. 방이 없으면 Optional.empty() 반환.
	 */
	public Optional<ChatRoom> findById(Long id) {
		String sql = "SELECT * FROM chat_rooms WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{id});
		
		if( parsedTable.isEmpty() )
			return Optional.empty();
		
		Map<String, Object> roomRow = parsedTable.get(0);
		return Optional.of(mapRowToRoom(roomRow));
	}
	
	/**
	 * 특정 유형의 채팅방 목록을 조회합니다.
	 *
	 * @param roomType 조회할 채팅방의 유형
	 * @return 해당 유형의 {@link ChatRoom} 객체 목록. 방이 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatRoom> findByRoomType(ChatRoom.RoomType roomType) {
		String sql = "SELECT * FROM chat_rooms WHERE room_type = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{roomType.name()});

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	/**
	 * 특정 소유자 ID를 가진 채팅방 목록을 조회합니다.
	 *
	 * @param ownerId 조회할 소유자의 ID
	 * @return 해당 소유자의 {@link ChatRoom} 객체 목록. 방이 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatRoom> findByOwnerId(Long ownerId) {
		String sql = "SELECT * FROM chat_rooms WHERE owner_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{ownerId});

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	/**
	 * 데이터베이스 행(Map)을 {@link ChatRoom} 엔티티로 매핑합니다.
	 *
	 * @param row 데이터베이스에서 조회된 단일 행을 나타내는 Map
	 * @return 매핑된 {@link ChatRoom} 객체
	 */
	private ChatRoom mapRowToRoom(Map<String, Object> row) {
		ChatRoom room = new ChatRoom((Long) row.get("room_id"), (String) row.get("room_name"));
		room.setRoom_type(ChatRoom.RoomType.valueOf((String)row.get("room_type")));
		room.setOwner((Long) row.get("owner_id"));
		room.setPassword_hash((String)row.get("password_hash"));
		Object createdAtObj = row.get("created_at");
		if (createdAtObj instanceof Timestamp) {
			room.setCreated_at(((Timestamp) createdAtObj).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		} else if (createdAtObj != null) {
			room.setCreated_at(createdAtObj.toString());
		} else {
			room.setCreated_at(null);
		}
		
		return room;
	}
	
	/**
	 * 특정 채팅방에 속한 사용자 목록을 {@link ChatRoomUserDto} 형태로 조회합니다.
	 * 각 사용자의 연결 상태 및 프로필 이미지 URL을 포함합니다.
	 *
	 * @param roomId 사용자 목록을 조회할 방의 ID
	 * @return 해당 방의 {@link ChatRoomUserDto} 객체 목록
	 */
	public List<ChatRoomUserDto> findUsersByRoomId(Long roomId){
		String sql = "SELECT u.user_id, cru.nickname, cru.role, u.profile_image_url "+
					"FROM users u INNER JOIN chat_room_users cru ON u.user_id = cru.user_id "+
					"WHERE room_id = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{roomId});
		
		return parsedTable.stream()
			.map(row -> new ChatRoomUserDto(
					(Long) row.get("user_id"),
					(String) row.get("nickname"),
					ChatRoomUserDto.UserType.valueOf((String) row.get("role")),
					roomSessionManager.getConnectedUsers(roomId).contains(row.get("user_id")) ? ChatRoomUserDto.ConnectType.CONNECT:ChatRoomUserDto.ConnectType.DISCONNECT,
							profileStaticUrlPrefix + "/" + (String) row.get("profile_image_url") 
			))
			.collect(Collectors.toList());
	}
	
	/**
	 * 채팅방을 저장합니다. 방 ID가 없으면 새로운 방을 삽입하고, ID가 있으면 기존 방을 업데이트합니다.
	 *
	 * @param room 저장할 {@link ChatRoom} 객체
	 * @return 저장 또는 업데이트된 {@link ChatRoom} 객체 (새 ID 및 생성 시간 포함)
	 */
	public ChatRoom save(ChatRoom room) {
		if( room.getId() == null ) {
			return insert(room);
		} else
			return update(room);
	}
	
	/**
	 * 새로운 채팅방을 데이터베이스에 삽입합니다.
	 * 생성된 방 ID와 생성 시간을 {@link ChatRoom} 객체에 채웁니다.
	 *
	 * @param room 삽입할 {@link ChatRoom} 객체
	 * @return ID와 생성 시간이 채워진 {@link ChatRoom} 객체
	 */
	private ChatRoom insert(ChatRoom room) {
		String sql = "INSERT INTO chat_rooms (room_name, room_type, owner_id, password_hash) VALUES ( ?, ?, ?, ? )";
		Map<String, Object> result = jdbcsql.executeUpdate(sql,
				new Object[]{room.getName(), room.getRoom_type().name(), room.getOwner(), room.getPassword_hash()}, 
				new String[]{"room_id"}, new String[] {"created_at"});
		
		if( result != null && result.containsKey("room_id")) { 
			room.setId( ((Number)result.get("room_id")).longValue()  ); 
			room.setCreated_at(((Timestamp)result.get("created_at")).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		return room;
	}
	
	/**
	 * 기존 채팅방 정보를 업데이트합니다.
	 * {@link ChatRoom#getChangedFields(ChatRoom)}를 사용하여 변경된 필드만 동적으로 SQL 쿼리를 구성하여 업데이트합니다.
	 *
	 * @param room 업데이트할 {@link ChatRoom} 객체
	 * @return 업데이트된 {@link ChatRoom} 객체
	 * @throws RuntimeException 방을 찾을 수 없거나 업데이트에 실패한 경우
	 */
	private ChatRoom update(ChatRoom room) {
		Optional<ChatRoom> fromdb = findByName(room.getName());
		if (fromdb.isEmpty())
			throw new RuntimeException("이름으로 방을 찾을 수 없습니다: " + room.getName());
		
		Map<String, Object> Changed = room.getChangedFields(fromdb.get());
		if (Changed.isEmpty()) { return room; } // 변경 사항 없으면 바로 리턴
		
		StringBuilder sql = new StringBuilder("UPDATE chat_rooms SET ");
		List<Object> values = new ArrayList<>();
		
		for( Map.Entry<String, Object> entry : Changed.entrySet() ) {
			sql.append(entry.getKey()).append(" = ?, ");
			values.add(entry.getValue());
		}
		sql.delete(sql.length() - 2, sql.length()); // 마지막 ", " 제거
		sql.append(" WHERE room_id = ?");
		values.add(room.getId());
		
		Long affectedRows = (long)jdbcsql.executeUpdate(sql.toString(), values.toArray(), null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ID " + room.getId() + "를 가진 채팅방을 찾을 수 없거나 삭제할 수 없습니다.");
		}
		
		return room;
	}
	
	/**
	 * 채팅방 ID를 기준으로 채팅방을 삭제합니다.
	 *
	 * @param id 삭제할 채팅방의 ID
	 * @throws RuntimeException 방을 찾을 수 없거나 삭제에 실패한 경우
	 */
	public void deleteById(Long id) {
		String sql = "DELETE FROM chat_rooms WHERE room_id = ?";
		Long affectedRows = (long)jdbcsql.executeUpdate(sql, new Object[]{id}, null, null).get("affected_rows");
		
		if (affectedRows == null || affectedRows == 0L) {
			throw new RuntimeException("ID " + id + "를 가진 채팅방을 찾을 수 없거나 삭제할 수 없습니다.");
		}
	}
	
	/**
	 * 특정 이름의 채팅방이 존재하는지 확인합니다.
	 *
	 * @param name 확인할 채팅방의 이름
	 * @return 해당 이름의 채팅방이 존재하면 true, 그렇지 않으면 false
	 */
	public boolean existsByName(String name) {
		String sql = "SELECT COUNT(1) FROM chat_rooms WHERE room_name = ?";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, new Object[]{name});

		if (parsedTable.isEmpty()) {
			return false;
		}

		long count = ((Number) parsedTable.get(0).values().iterator().next()).longValue();
		return count > 0;
	}
	
	/**
	 * 모든 채팅방 목록을 조회합니다.
	 *
	 * @return 모든 {@link ChatRoom} 객체 목록. 방이 없으면 빈 리스트를 반환합니다.
	 */
	public List<ChatRoom> findAll() {
		String sql = "SELECT * FROM chat_rooms";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
				.map(this::mapRowToRoom)
				.collect(Collectors.toList());
	}
	
	/**
	 * 모든 채팅방 목록과 각 방의 사용자 수를 함께 조회합니다.
	 * {@link ChatRoomListDto} 형태로 반환됩니다.
	 *
	 * @return {@link ChatRoomListDto} 객체 목록
	 */
	public List<ChatRoomListDto> findAllWithCount(){
		String sql = "SELECT r.room_id, r.room_name, r.room_type, u.nickname AS ownerName, COUNT(c.user_id) AS userCount "
					+ "FROM chat_rooms r "
					+ "LEFT JOIN chat_room_users c ON r.room_id = c.room_id "
					+ "LEFT JOIN users u ON r.owner_id = u.user_id "
					+ "GROUP BY r.room_id, r.room_name, r.room_type, u.nickname;";
		
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, null);
		
		if (parsedTable.isEmpty()) {
			return Collections.emptyList();
		}

		return parsedTable.stream()
								.map(row -> {
									Number userCount = (Number) row.get("userCount");
									return new ChatRoomListDto(
											(Long) row.get("room_id"),
											(String) row.get("room_name"),
											(String) row.get("room_type"),
											(String) row.get("ownerName"),
											userCount != null ? userCount.intValue() : 0,
											null, false);
								}) // connCount와 isMember는 이 쿼리에서 알 수 없으므로 null/false 처리
				.collect(Collectors.toList());
	}
	
	/**
	 * 채팅방 ID를 기준으로 단일 채팅방의 상세 정보를 {@link ChatRoomListDto} 형태로 조회합니다.
	 *
	 * @param roomId 조회할 채팅방의 ID
	 * @return {@link ChatRoomListDto} 객체를 포함하는 {@link Optional}. 방이 없으면 Optional.empty() 반환.
	 */
	public Optional<ChatRoomListDto> findRoomDtoById(Long roomId) {
        String sql = "SELECT r.room_id as id, r.room_name as name, r.room_type, u.nickname as owner_name, " +
                     "(SELECT COUNT(*) FROM chat_room_users cru WHERE cru.room_id = r.room_id) as user_count " +
                     "FROM chat_rooms r JOIN users u ON r.owner_id = u.user_id " +
                     "WHERE r.room_id = ?";
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, new Object[]{roomId});
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> row = rows.get(0);
        return Optional.of(new ChatRoomListDto(
            (Long) row.get("id"),
            (String) row.get("name"),
            (String) row.get("room_type"),
            (String) row.get("owner_name"),
            ((Number) row.get("user_count")).intValue(),
            0, // connected_users는 이 쿼리에서 알 수 없으므로 0으로 설정
            false // isMember는 이 쿼리에서 알 수 없으므로 false로 설정
        ));
    }
	
	/**
	 * 전체 채팅방의 개수를 반환합니다.
	 *
	 * @return 전체 채팅방의 개수
	 */
	public long count() {
		String sql = "SELECT COUNT(*) FROM chat_rooms";
		List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql, null);

		if (parsedTable.isEmpty()) {
			return 0L;
		}
		
		return ((Number) parsedTable.get(0).values().iterator().next()).longValue();
	}
	
	/**
	 * 특정 채팅방에 속한 사용자 수를 반환합니다.
	 *
	 * @param roomId 사용자 수를 조회할 방의 ID
	 * @return 해당 방의 사용자 수
	 */
	public int countUsersByRoomId(Long roomId) {
	    String sql = "SELECT count(user_id) FROM chat_room_users WHERE room_id = ?";
	    List<Map<String, Object>> result = jdbcsql.executeSelect(sql, new Object[]{roomId});
	    if (result.isEmpty()) {
	        return 0;
	    }
	    return ((Number) result.get(0).values().iterator().next()).intValue();
	}
}
