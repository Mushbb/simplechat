package com.example.simplechat.service;

import com.example.simplechat.dto.FriendResponseDto;
import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FriendshipRepository;
import com.example.simplechat.repository.NotificationRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 알림을 관리하고, 알림 관련 동작(수락, 거부)을 처리하는 서비스 클래스입니다.
 * 친구 요청, 방 초대 등 다양한 유형의 알림을 생성, 조회, 업데이트 및 삭제합니다.
 */
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
    private final ObjectMapper objectMapper; // ObjectMapper 주입

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    /**
     * 새로운 알림을 저장합니다.
     *
     * @param notification 저장할 {@link Notification} 객체
     * @return 저장된 {@link Notification} 객체
     */
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    /**
     * 알림 ID를 기준으로 단일 알림을 조회합니다.
     *
     * @param notificationId 조회할 알림의 ID
     * @return 알림을 포함하는 {@link Optional<Notification>} 객체. 알림이 없으면 Optional.empty() 반환.
     */
    public Optional<Notification> findById(long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    /**
     * 특정 사용자에게 전송된 알림 목록을 조회합니다. 읽음 상태로 필터링할 수 있습니다.
     *
     * @param receiverId 알림을 조회할 수신자의 ID
     * @param isRead (선택 사항) 읽음 상태로 필터링할지 여부 (true: 읽음, false: 안 읽음, null: 필터링 안 함)
     * @return 조회된 {@link Notification} 객체 목록
     */
    public List<Notification> getNotificationsForUser(long receiverId, Boolean isRead) {
        return notificationRepository.findByReceiverId(receiverId, isRead);
    }

    /**
     * 특정 알림들을 읽음 상태로 표시합니다.
     *
     * @param notificationIds 읽음 상태로 표시할 알림 ID 목록
     * @param receiverId 알림을 받은 수신자의 ID (권한 확인용)
     */
    public void markNotificationsAsRead(List<Long> notificationIds, Long receiverId) {
        notificationRepository.updateIsReadStatus(notificationIds, receiverId, true);
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 삭제할 알림의 ID
     */
    public void deleteNotification(long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * 사용자 알림을 수락하고, 알림 유형에 따라 적절한 후속 조치를 취합니다.
     * 예를 들어, 친구 요청 수락 시 친구 관계를 생성하고, 방 초대 수락 시 방에 참여시킵니다.
     *
     * @param notificationId 수락할 알림의 ID
     * @param userId 알림을 수락하는 사용자의 ID
     * @throws RegistrationException 알림을 찾을 수 없거나 사용자에게 권한이 없는 경우
     */
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
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "요청자를 찾을 수 없습니다."));
                User accepter = userRepository.findById(userId)
                        .orElseThrow(() -> new RegistrationException("NOT_FOUND", "수락자를 찾을 수 없습니다."));

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
                User user = userRepository.findById(userId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "사용자를 찾을 수 없습니다."));
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

    /**
     * 사용자 알림을 거부하고, 해당 알림을 삭제합니다.
     *
     * @param notificationId 거부할 알림의 ID
     * @param userId 알림을 거부하는 사용자의 ID
     * @throws RegistrationException 알림을 찾을 수 없거나 사용자에게 권한이 없는 경우
     */
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

    /**
     * 친구 상태 업데이트에 대한 WebSocket 알림을 특정 사용자에게 전송합니다.
     *
     * @param targetUser 알림을 받을 대상 사용자
     * @param friendUser 친구 관계의 상대방 사용자
     * @param updateType 업데이트 유형 (예: "FRIEND_ADDED", "FRIEND_ACCEPTED")
     * @throws RuntimeException JSON 직렬화 실패 시
     */
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

        try {
            String payload = objectMapper.writeValueAsString(Map.of( // 주입된 ObjectMapper 사용
                    "type", updateType,
                    "friend", friendDto
            ));
            messagingTemplate.convertAndSendToUser(
                    targetUser.getUsername(),
                    "/queue/notifications",
                    payload
            );
        } catch (JsonProcessingException e) { // Exception -> JsonProcessingException으로 변경
            throw new RuntimeException("친구 업데이트 알림 직렬화에 실패했습니다.", e);
        }
    }
}
