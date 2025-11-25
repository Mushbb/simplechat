package com.example.simplechat.repository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * chat_room_users 테이블에 대한 데이터 접근을 담당하는 리포지토리입니다.
 * 사용자와 채팅방 간의 관계(멤버십)를 관리하며, JDBC_SQL을 통해 데이터베이스와 상호작용합니다.
 */
@RequiredArgsConstructor
@Repository
public class RoomUserRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoomUserRepository.class);
	private final JDBC_SQL jdbcsql;
	
	/**
     * 특정 사용자가 참여하고 있는 모든 채팅방의 기본 정보를 조회합니다.
     * 방 정보와 방 개설자의 닉네임을 함께 가져옵니다.
     *
     * @param userId 사용자의 ID
     * @return 각 방의 정보를 담은 Map의 리스트
     */
    public List<Map<String, Object>> findRoomsByUserId(Long userId) {
        String sql = "SELECT r.room_id, r.room_name, r.room_type, u.nickname as owner_name " +
                     "FROM chat_rooms r " +
                     "INNER JOIN chat_room_users cru ON r.room_id = cru.room_id " +
                     "LEFT JOIN users u ON r.owner_id = u.user_id " +
                     "WHERE cru.user_id = ?";
        
        return jdbcsql.executeSelect(sql, new Object[]{userId});
    }
	
    /**
     * 특정 사용자를 특정 채팅방에 지정된 역할과 닉네임으로 추가합니다.
     *
     * @param userId   추가할 사용자의 ID
     * @param roomId   대상 채팅방의 ID
     * @param nickname 방에서 사용할 사용자의 초기 닉네임
     * @param role     사용자에게 부여할 역할 (예: "USER", "ADMIN")
     * @throws RuntimeException 사용자 추가에 실패한 경우
     */
    public void save(Long userId, Long roomId, String nickname, String role) {
        String sql = "INSERT INTO chat_room_users (user_id, room_id, nickname, role) VALUES (?, ?, ?, ?)";
        Map<String, Object> result = jdbcsql.executeUpdate(sql,
                new Object[]{userId, roomId, nickname, role},
                null, null);

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            throw new RuntimeException("사용자 " + userId + "를 방 " + roomId + "에 추가하지 못했습니다.");
        }
    }

    /**
     * 특정 방에서 사용자의 닉네임을 변경합니다.
     *
     * @param userId      닉네임을 변경할 사용자의 ID
     * @param roomId      대상 채팅방의 ID
     * @param newNickname 새로운 닉네임
     */
    public void updateNickname(Long userId, Long roomId, String newNickname) {
        String sql = "UPDATE chat_room_users SET nickname = ? WHERE user_id = ? AND room_id = ?";
        Map<String, Object> result = jdbcsql.executeUpdate(sql,
                new Object[]{newNickname, userId, roomId},
                null, null);

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            logger.warn("경고: 방 {}의 사용자 {}의 닉네임이 업데이트되지 않았습니다. 사용자가 방에 없을 수 있습니다.", roomId, userId);
        }
    }
    
    /**
     * 특정 채팅방에서 사용자의 닉네임을 조회합니다.
     *
     * @param userId 닉네임을 조회할 사용자의 ID
     * @param roomId 대상 채팅방의 ID
     * @return 사용자의 닉네임 문자열 또는 찾을 수 없는 경우 null
     */
    public String getNickname(Long userId, Long roomId) {
    	String sql = "SELECT nickname FROM chat_room_users WHERE user_id = ? AND room_id = ?";
    	
    	List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql,
                new Object[]{userId, roomId});

        if (parsedTable.isEmpty()) {
            return null;
        }
        
        return (String) parsedTable.get(0).values().iterator().next();
    }

    /**
     * 특정 사용자를 특정 채팅방에서 제거합니다.
     *
     * @param userId 제거할 사용자의 ID
     * @param roomId 대상 채팅방의 ID
     */
    public void delete(Long userId, Long roomId) {
        String sql = "DELETE FROM chat_room_users WHERE user_id = ? AND room_id = ?";
        Map<String, Object> result = jdbcsql.executeUpdate(sql,
                new Object[]{userId, roomId},
                null, null);

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            logger.warn("경고: 사용자 {}가 방 {}에 없거나 제거할 수 없었습니다.", userId, roomId);
        }
    }

    /**
     * 특정 사용자가 특정 채팅방에 참여하고 있는지 확인합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @param roomId 대상 채팅방의 ID
     * @return 참여하고 있으면 true, 아니면 false
     */
    public boolean exists(Long userId, Long roomId) {
        String sql = "SELECT COUNT(1) FROM chat_room_users WHERE user_id = ? AND room_id = ?";
        if( userId == null ) { // userId가 null인 경우 존재하지 않는 것으로 처리
        	return false;
        }
        
        List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql,
                new Object[]{userId, roomId});

        if (parsedTable.isEmpty()) { // 결과 자체가 없는 경우 (쿼리 오류 등)
            return false;
        }

        long count = ((Number) parsedTable.get(0).values().iterator().next()).longValue();
        return count > 0;
    }
    
    /**
     * 특정 채팅방에서 사용자의 역할을 조회합니다.
     *
     * @param userId 역할을 조회할 사용자의 ID
     * @param roomId 대상 채팅방의 ID
     * @return 사용자의 역할 문자열 (예: "ADMIN", "MEMBER") 또는 찾을 수 없는 경우 null
     */
    public String getRole(Long userId, Long roomId) {
    	String sql = "SELECT role FROM chat_room_users WHERE user_id = ? AND room_id = ?";
        List<Map<String, Object>> parsedTable = jdbcsql.executeSelect(sql,
                new Object[]{userId, roomId});

        if (parsedTable.isEmpty()) {
            return null;
        }
        
        return (String) parsedTable.get(0).values().iterator().next();
    }

    /**
     * 특정 채팅방에 속한 모든 사용자들을 제거합니다. (방 삭제 시 사용)
     *
     * @param roomId 사용자들을 제거할 방의 ID
     */
    public void deleteByRoomId(Long roomId) {
        String sql = "DELETE FROM chat_room_users WHERE room_id = ?";
        jdbcsql.executeUpdate(sql, new Object[]{roomId}, null, null);
    }

    /**
     * 특정 사용자를 모든 채팅방에서 제거합니다. (사용자 삭제 시 사용)
     *
     * @param userId 제거할 사용자의 ID
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM chat_room_users WHERE user_id = ?";
        jdbcsql.executeUpdate(sql, new Object[]{userId}, null, null);
    }
}
