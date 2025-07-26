package com.example.simplechat.repository;

import com.example.sql.JDBC_SQL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * chat_room_users 테이블에 대한 데이터 접근을 담당하는 리포지토리입니다.
 * 사용자와 채팅방 간의 관계(멤버십)를 관리합니다.
 */
@Repository
public class RoomUserRepository {

    /**
     * 특정 사용자를 특정 채팅방에 지정된 역할과 닉네임으로 추가합니다.
     *
     * @param userId   추가할 사용자의 ID
     * @param roomId   대상 채팅방의 ID
     * @param nickname 방에서 사용할 사용자의 초기 닉네임
     * @param role     사용자에게 부여할 역할 (예: "USER", "ADMIN")
     */
    public void save(Long userId, Long roomId, String nickname, String role) {
        String sql = "INSERT INTO chat_room_users (user_id, room_id, nickname, role) VALUES (?, ?, ?, ?)";
        Map<String, Object> result = JDBC_SQL.executeUpdate(sql,
                new String[]{String.valueOf(userId), String.valueOf(roomId), nickname, role},
                null, null); // 생성된 키를 반환받을 필요 없음

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            throw new RuntimeException("Failed to add user " + userId + " to room " + roomId);
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
        Map<String, Object> result = JDBC_SQL.executeUpdate(sql,
                new String[]{newNickname, String.valueOf(userId), String.valueOf(roomId)},
                null, null);

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            // 닉네임 업데이트가 실패한 경우, 경고를 출력하거나 예외를 던질 수 있습니다.
            // 여기서는 경고만 출력합니다.
            System.out.println("Warn: Nickname for user " + userId + " in room " + roomId + " was not updated. The user may not be in the room.");
        }
    }

    /**
     * 특정 사용자를 특정 채팅방에서 제거합니다.
     *
     * @param userId 제거할 사용자의 ID
     * @param roomId 대상 채팅방의 ID
     */
    public void delete(Long userId, Long roomId) {
        String sql = "DELETE FROM chat_room_users WHERE user_id = ? AND room_id = ?";
        Map<String, Object> result = JDBC_SQL.executeUpdate(sql,
                new String[]{String.valueOf(userId), String.valueOf(roomId)},
                null, null);

        Long affectedRows = (Long) result.get("affected_rows");
        if (affectedRows == null || affectedRows == 0) {
            System.out.println("Warn: User " + userId + " was not in room " + roomId + ", or could not be removed.");
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
        List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql,
                new String[]{String.valueOf(userId), String.valueOf(roomId)});

        if (parsedTable.isEmpty()) {
            return false;
        }

        int count = (int) parsedTable.get(0).values().iterator().next();
        return count > 0;
    }
    
    public String getRole(Long userId, Long roomId) {
    	String sql = "SELECT role FROM chat_room_users WHERE user_id = ? AND room_id = ?";
        List<Map<String, Object>> parsedTable = JDBC_SQL.executeSelect(sql,
                new String[]{String.valueOf(userId), String.valueOf(roomId)});

        if (parsedTable.isEmpty()) {
            return null;
        }
        
        return (String) parsedTable.get(0).values().iterator().next();
    }
}
