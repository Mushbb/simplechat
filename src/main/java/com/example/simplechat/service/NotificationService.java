package com.example.simplechat.service;

import com.example.simplechat.dto.FriendResponseDto;
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FriendshipRepository;
import com.example.simplechat.repository.NotificationRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final RoomUserRepository roomUserRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;


    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;


    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Optional<Notification> findById(long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    public List<Notification> getNotificationsForUser(long receiverId, Boolean isRead) {
        return notificationRepository.findByReceiverId(receiverId, isRead);
    }

    public void markNotificationsAsRead(List<Long> notificationIds, Long receiverId) {
        notificationRepository.updateIsReadStatus(notificationIds, receiverId, true);
    }

    public void deleteNotification(long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void acceptNotification(Long notificationId, Long userId) {
        Notification notification = findById(notificationId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "알림을 찾을 수 없습니다."));

        if (!notification.getReceiverId().equals(userId)) {
            throw new RegistrationException("FORBIDDEN", "권한이 없습니다.");
        }

        switch (notification.getNotificationType()) {
            case FRIEND_REQUEST:
                long requesterId = notification.getRelatedEntityId();
                User requester = userRepository.findById(requesterId)
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "Requester not found."));
                User accepter = userRepository.findById(userId)
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "Accepter not found."));

                // Friendship 테이블에 ACCEPTED 상태로 저장
                Friendship friendship = new Friendship(requesterId, userId, Friendship.Status.ACCEPTED, null, 0);
                friendshipRepository.save(friendship);

                // 알림을 보낸 사람에게: 상대방이 요청을 수락했음을 알림
                sendFriendUpdateNotification(requester, accepter, "FRIEND_ACCEPTED");

                // 알림을 받은 사람(수락한 사람)에게: 새로운 친구가 추가되었음을 알림
                sendFriendUpdateNotification(accepter, requester, "FRIEND_ADDED");
                break;

            case ROOM_INVITATION:
                long roomId = notification.getRelatedEntityId();
                User user = userRepository.findById(userId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found."));
                // room_user 테이블에 저장 (실제 방 참여)
                roomUserRepository.save(userId, roomId, user.getNickname(), "MEMBER");
                // 방에 이벤트 발행
                eventPublisher.publishEvent(new UserEnteredRoomEvent(this, user, roomId, com.example.simplechat.dto.UserEventDto.UserType.MEMBER));
                break;

            default:
                break;
        }
        // 처리된 알림 삭제
        deleteNotification(notificationId);
    }

    @Transactional
    public void rejectNotification(Long notificationId, Long userId) {
        Notification notification = findById(notificationId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "알림을 찾을 수 없습니다."));

        if (!notification.getReceiverId().equals(userId)) {
            throw new RegistrationException("FORBIDDEN", "권한이 없습니다.");
        }

        // 알림 삭제
        deleteNotification(notificationId);
    }

    private void sendFriendUpdateNotification(User targetUser, User friendUser, String updateType) {
        String friendProfileUrl = friendUser.getProfile_image_url();
        friendProfileUrl = (friendProfileUrl != null && !friendProfileUrl.isBlank())
                ? profileStaticUrlPrefix + "/" + friendProfileUrl
                : profileStaticUrlPrefix + "/default.png";

        FriendResponseDto.ConnectType conn = presenceService.isUserOnline(friendUser.getId())
                ? FriendResponseDto.ConnectType.CONNECT
                : FriendResponseDto.ConnectType.DISCONNECT;

        FriendResponseDto friendDto = new FriendResponseDto(
                friendUser.getId(),
                friendUser.getUsername(),
                friendUser.getNickname(),
                friendProfileUrl,
                "ACCEPTED",
                conn
        );

        // WebSocket을 통해 특정 사용자에게만 알림 전송
        try {
            String payload = new ObjectMapper().writeValueAsString(Map.of(
                    "type", updateType, // 예: "FRIEND_ADDED" 또는 "FRIEND_ACCEPTED"
                    "friend", friendDto
            ));
            messagingTemplate.convertAndSendToUser(
                    targetUser.getUsername(),
                    "/queue/notifications",
                    payload
            );
        } catch (Exception e) {
            // 직렬화 실패 시 예외 처리
            throw new RuntimeException("Failed to serialize friend update notification", e);
        }
    }
}
