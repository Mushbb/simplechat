package com.example.simplechat.repository;

import com.example.simplechat.model.Friendship;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FriendshipRepository {

    private final JDBC_SQL jdbcsql;

    public Friendship save(Friendship friendship) {
        String sql = "INSERT INTO friendships (user_id_1, user_id_2, status) VALUES (?, ?, ?)";
        Object[] params = {friendship.userId1(), friendship.userId2(), friendship.status()};
        Map<String, Object> result = jdbcsql.executeUpdate(sql, params, new String[]{"relation_id", "created_at"}, null);

        if (result != null && result.containsKey("relation_id")) {
            return new Friendship(
                friendship.userId1(),
                friendship.userId2(),
                friendship.status(),
                ((Timestamp) result.get("created_at")).toLocalDateTime(),
                ((Number) result.get("relation_id")).longValue()
            );
        }
        return friendship; // Should not happen if DB insert is successful
    }

    public void updateStatus(long userId1, long userId2, String status) {
        String sql = "UPDATE friendships SET status = ? WHERE user_id_1 = ? AND user_id_2 = ?";
        Object[] params = {status, userId1, userId2};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    public void delete(long userId1, long userId2) {
        String sql = "DELETE FROM friendships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)";
        Object[] params = {userId1, userId2, userId2, userId1};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    public void deleteByRequesterAndReceiver(long requesterId, long receiverId) {
        String sql = "DELETE FROM friendships WHERE user_id_1 = ? AND user_id_2 = ?";
        Object[] params = {requesterId, receiverId};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    public List<Friendship> findByUserIdAndStatus(long userId, String status) {
        String sql = "SELECT * FROM friendships WHERE (user_id_1 = ? OR user_id_2 = ?) AND status = ?";
        Object[] params = {userId, userId, status};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        return rows.stream().map(this::mapRowToFriendship).collect(Collectors.toList());
    }

    public List<Friendship> findIncomingPendingRequests(long userId) {
        String sql = "SELECT * FROM friendships WHERE user_id_2 = ? AND status = 'PENDING'";
        Object[] params = {userId};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        return rows.stream().map(this::mapRowToFriendship).collect(Collectors.toList());
    }

    public Optional<Friendship> findByUsers(long userId1, long userId2) {
        String sql = "SELECT * FROM friendships WHERE (user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?)";
        Object[] params = {userId1, userId2, userId2, userId1};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRowToFriendship(rows.get(0)));
    }

    private Friendship mapRowToFriendship(Map<String, Object> row) {
        return new Friendship(
                ((Number) row.get("user_id_1")).longValue(),
                ((Number) row.get("user_id_2")).longValue(),
                (String) row.get("status"),
                row.get("created_at") != null ? ((Timestamp) row.get("created_at")).toLocalDateTime() : null,
                ((Number) row.get("relation_id")).longValue()
        );
    }
}
