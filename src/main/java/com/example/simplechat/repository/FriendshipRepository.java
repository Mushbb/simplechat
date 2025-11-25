package com.example.simplechat.repository;

import com.example.simplechat.model.Friendship;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 친구 관계(Friendship) 엔티티의 영속성(데이터베이스 CRUD)을 관리하는 리포지토리 클래스입니다.
 * {@link JDBC_SQL}을 사용하여 데이터베이스와 상호작용합니다.
 */
@Repository
@RequiredArgsConstructor
public class FriendshipRepository {
    private final JDBC_SQL jdbcsql;

    /**
     * 새로운 친구 관계를 저장합니다.
     *
     * @param friendship 저장할 {@link Friendship} 객체
     * @return ID와 생성 시간을 포함하여 저장된 {@link Friendship} 객체
     */
    public Friendship save(Friendship friendship) {
        String sql = "INSERT INTO friendships (user_id_1, user_id_2, status) VALUES (?, ?, ?)";
        
        Object[] params = {friendship.getUserId1(), friendship.getUserId2(), friendship.getStatus().name()};
        Map<String, Object> result = jdbcsql.executeUpdate(sql, params, new String[]{"relation_id"}, new String[]{"created_at"});

        if (result != null && result.containsKey("relation_id")) {
            friendship.setRelationId(((Number) result.get("relation_id")).longValue());
            friendship.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        }
        return friendship;
    }

    /**
     * 특정 사용자들 간의 친구 관계 상태를 업데이트합니다.
     * `user_id_1`과 `user_id_2`의 순서에 관계없이 친구 관계를 찾아서 업데이트합니다.
     *
     * @param userId1 친구 관계에 있는 첫 번째 사용자의 ID
     * @param userId2 친구 관계에 있는 두 번째 사용자의 ID
     * @param status 새로운 친구 관계 상태 (예: "ACCEPTED", "DECLINED")
     */
    public void updateStatus(long userId1, long userId2, String status) {
        String sql = "UPDATE friendships SET status = ? WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)";
        Object[] params = {status, userId1, userId2, userId2, userId1};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    /**
     * 특정 사용자들 간의 친구 관계를 삭제합니다. (양방향 삭제)
     *
     * @param userId1 삭제할 친구 관계에 있는 첫 번째 사용자의 ID
     * @param userId2 삭제할 친구 관계에 있는 두 번째 사용자의 ID
     */
    public void delete(long userId1, long userId2) {
        String sql = "DELETE FROM friendships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)";
        Object[] params = {userId1, userId2, userId2, userId1};
        jdbcsql.executeUpdate(sql, params, null, null);
    }
    
    /**
     * 요청자와 수신자 ID를 기반으로 친구 관계를 삭제합니다.
     *
     * @param requesterId 친구 관계 요청자의 ID
     * @param receiverId 친구 관계 수신자의 ID
     */
    public void deleteByRequesterAndReceiver(long requesterId, long receiverId) {
        String sql = "DELETE FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
        Object[] params = {requesterId, receiverId};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    /**
     * 특정 사용자의 특정 상태를 가진 친구 관계 목록을 조회합니다.
     *
     * @param userId 친구 관계를 조회할 사용자의 ID
     * @param status 조회할 친구 관계의 상태 (예: PENDING, ACCEPTED)
     * @return 조회된 {@link Friendship} 객체 목록
     */
    public List<Friendship> findByUserIdAndStatus(long userId, Friendship.Status status) {
        String sql = "SELECT * FROM friendships WHERE (user_id_1 = ? OR user_id_2 = ?) AND status = ?";
        Object[] params = {userId, userId, status.name()};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        return rows.stream().map(this::mapRowToFriendship).collect(Collectors.toList());
    }

    /**
     * 특정 사용자에게 온 대기 중인(PENDING) 친구 요청 목록을 조회합니다.
     *
     * @param userId 친구 요청을 받은 사용자의 ID
     * @return 대기 중인 {@link Friendship} 객체 목록
     */
    public List<Friendship> findIncomingPendingRequests(long userId) {
        String sql = "SELECT * FROM friendships WHERE user_id_2 = ? AND status = 'PENDING'";
        Object[] params = {userId};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        return rows.stream().map(this::mapRowToFriendship).collect(Collectors.toList());
    }

    /**
     * 두 사용자 간의 친구 관계를 조회합니다. 순서에 상관없이 검색합니다.
     *
     * @param userId1 친구 관계에 있는 첫 번째 사용자의 ID
     * @param userId2 친구 관계에 있는 두 번째 사용자의 ID
     * @return 두 사용자 간의 친구 관계를 담고 있는 {@link Optional<Friendship>} 객체
     */
    public Optional<Friendship> findByUsers(long userId1, long userId2) {
        String sql = "SELECT * FROM friendships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)";
        Object[] params = {userId1, userId2, userId2, userId1};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRowToFriendship(rows.get(0)));
    }
    
    /**
     * 데이터베이스 행(Map)을 {@link Friendship} 엔티티로 매핑합니다.
     *
     * @param row 데이터베이스에서 조회된 단일 행을 나타내는 Map
     * @return 매핑된 {@link Friendship} 객체
     */
    private Friendship mapRowToFriendship(Map<String, Object> row) {
        return new Friendship(
                ((Number) row.get("user_id_1")).longValue(),
                ((Number) row.get("user_id_2")).longValue(),
                Friendship.Status.valueOf( ((String) row.get("status")) ),
                ((Timestamp) row.get("created_at")).toLocalDateTime(),
                ((Number) row.get("relation_id")).longValue()
        );
    }
}
