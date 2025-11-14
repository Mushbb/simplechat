package com.example.simplechat.repository;

import com.example.simplechat.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final JDBC_SQL jdbcsql;

    public Notification save(Notification notification) {
        String sql = "INSERT INTO notifications (receiver_id, notification_type, content, related_entity_id, metadata) VALUES (?, ?, ?, ?, ?)";
        Object[] params = {
            notification.getReceiverId(),
            notification.getNotificationType().name(),
            notification.getContent(),
            notification.getRelatedEntityId(),
            notification.getMetadata()
        };
        Map<String, Object> result = jdbcsql.executeUpdate(sql, params, new String[]{"notification_id"}, new String[]{"created_at"});

        if (result != null && result.containsKey("notification_id")) {
            notification.setId(((Number) result.get("notification_id")).longValue());
            notification.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        }
        return notification;
    }

    public Optional<Notification> findById(long notificationId) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        Object[] params = {notificationId};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRowToNotification(rows.get(0)));
    }

    public List<Notification> findByReceiverId(long receiverId, Boolean isRead) {
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE receiver_id = ?");
        if (isRead != null) {
            sql.append(" AND is_read = ?");
        }
        sql.append(" ORDER BY created_at DESC");

        Object[] params;
        if (isRead != null) {
            params = new Object[]{receiverId, isRead};
        } else {
            params = new Object[]{receiverId};
        }

        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql.toString(), params);
        return rows.stream().map(this::mapRowToNotification).collect(Collectors.toList());
    }

    public void updateIsReadStatus(List<Long> notificationIds, Long receiverId, boolean isRead) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        String inSql = notificationIds.stream()
                                      .map(id -> "?")
                                      .collect(Collectors.joining(", "));
        String sql = String.format("UPDATE notifications SET is_read = ? WHERE notification_id IN (%s) AND receiver_id = ?", inSql);
        
        Object[] params = new Object[notificationIds.size() + 2];
        params[0] = isRead;
        for (int i = 0; i < notificationIds.size(); i++) {
            params[i + 1] = notificationIds.get(i);
        }
        params[notificationIds.size() + 1] = receiverId;

        jdbcsql.executeUpdate(sql, params, null, null);
    }

    public void deleteById(long notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        Object[] params = {notificationId};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    private Notification mapRowToNotification(Map<String, Object> row) {
        return new Notification(
            ((Number) row.get("notification_id")).longValue(),
            ((Number) row.get("receiver_id")).longValue(),
            Notification.NotificationType.valueOf((String) row.get("notification_type")),
            (String) row.get("content"),
            row.get("related_entity_id") != null ? ((Number) row.get("related_entity_id")).longValue() : null,
            (String) row.get("metadata"),
            (Boolean) row.get("is_read"),
            ((Timestamp) row.get("created_at")).toLocalDateTime()
        );
    }
}