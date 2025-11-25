package com.example.simplechat.repository;

import com.example.simplechat.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 알림(Notification) 엔티티의 영속성(데이터베이스 CRUD)을 관리하는 리포지토리 클래스입니다.
 * {@link JDBC_SQL}을 사용하여 데이터베이스와 상호작용합니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final JDBC_SQL jdbcsql;

    /**
     * 새로운 알림을 저장합니다.
     *
     * @param notification 저장할 {@link Notification} 객체
     * @return ID와 생성 시간을 포함하여 저장된 {@link Notification} 객체
     */
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

    /**
     * 알림 ID를 기준으로 단일 알림을 조회합니다.
     *
     * @param notificationId 조회할 알림의 ID
     * @return 알림을 포함하는 {@link Optional<Notification>} 객체. 알림이 없으면 Optional.empty() 반환.
     */
    public Optional<Notification> findById(long notificationId) {
        String sql = "SELECT * FROM notifications WHERE notification_id = ?";
        Object[] params = {notificationId};
        List<Map<String, Object>> rows = jdbcsql.executeSelect(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRowToNotification(rows.get(0)));
    }

    /**
     * 특정 수신자의 알림 목록을 조회합니다. 읽음 상태(isRead)로 필터링할 수 있습니다.
     *
     * @param receiverId 알림을 조회할 수신자의 ID
     * @param isRead (선택 사항) 읽음 상태로 필터링할지 여부 (true: 읽음, false: 안 읽음, null: 필터링 안 함)
     * @return 조회된 {@link Notification} 객체 목록
     */
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

    /**
     * 지정된 알림 ID 목록에 해당하는 알림들의 읽음 상태를 업데이트합니다.
     *
     * @param notificationIds 읽음 상태를 업데이트할 알림 ID 목록
     * @param receiverId 알림을 받은 수신자의 ID (보안 검증용)
     * @param isRead 설정할 읽음 상태 (true: 읽음, false: 안 읽음)
     */
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

    /**
     * 알림 ID를 기준으로 알림을 삭제합니다.
     *
     * @param notificationId 삭제할 알림의 ID
     */
    public void deleteById(long notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        Object[] params = {notificationId};
        jdbcsql.executeUpdate(sql, params, null, null);
    }

    /**
     * 데이터베이스 행(Map)을 {@link Notification} 엔티티로 매핑합니다.
     *
     * @param row 데이터베이스에서 조회된 단일 행을 나타내는 Map
     * @return 매핑된 {@link Notification} 객체
     */
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