package com.example.simplechat.service;

import com.example.simplechat.dto.FriendResponseDto;
import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FriendshipRepository;
import com.example.simplechat.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 간의 친구 관계를 관리하는 서비스 클래스입니다.
 * 친구 요청 전송, 친구 목록 조회, 친구 삭제, 친구 관계 상태 확인 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    /**
     * 친구 요청을 보냅니다.
     * 자기 자신에게 친구 요청을 보내거나, 이미 친구 관계 또는 요청이 존재하는지 확인합니다.
     * 요청 성공 시 수신자에게 알림을 생성하고 실시간으로 전송합니다.
     *
     * @param senderId 친구 요청을 보내는 사용자의 ID
     * @param receiverId 친구 요청을 받을 사용자의 ID
     * @throws RegistrationException 자기 자신에게 요청을 보내거나, 이미 친구 관계 또는 요청이 존재하는 경우
     * @throws RegistrationException 발신자 또는 수신자 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void sendFriendRequest(long senderId, long receiverId) {
        if (senderId == receiverId) {
            throw new RegistrationException("BAD_REQUEST", "자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "발신자를 찾을 수 없습니다."));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "수신자를 찾을 수 없습니다."));

        friendshipRepository.findByUsers(senderId, receiverId).ifPresent(f -> {
            throw new RegistrationException("CONFLICT", "친구 관계 또는 요청이 이미 존재합니다.");
        });

        String content = sender.getNickname() + "님이 친구 요청을 보냈습니다.";
        Notification notification = new Notification(receiverId, Notification.NotificationType.FRIEND_REQUEST, content, senderId, null);
        notificationService.save(notification);

        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/notifications",
                NotificationDto.from(notification)
        );
    }

    /**
     * 특정 사용자의 수락된 친구 목록을 조회합니다.
     * 각 친구의 온라인 상태 및 프로필 이미지 정보를 포함합니다.
     *
     * @param userId 친구 목록을 조회할 사용자의 ID
     * @return {@link FriendResponseDto} 객체 목록
     */
    public List<FriendResponseDto> getFriends(long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, Friendship.Status.ACCEPTED);

        return friendships.stream()
                .map(friendship -> {
                    long friendId = friendship.getUserId1() == userId ? friendship.getUserId2() : friendship.getUserId1();
                    User friend = userRepository.findById(friendId)
                            .orElseThrow(() -> new IllegalStateException("친구 사용자를 찾을 수 없습니다."));
                    String url = friend.getProfile_image_url();
                    friend.setProfile_image_url(url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url : profileStaticUrlPrefix + "/default.png");
                    FriendResponseDto.ConnectType conn = presenceService.isUserOnline(friendId) ? FriendResponseDto.ConnectType.CONNECT : FriendResponseDto.ConnectType.DISCONNECT;

                    return FriendResponseDto.from(friend, "ACCEPTED", conn);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자들 간의 친구 관계를 삭제합니다.
     *
     * @param removerId 친구 관계를 삭제하는 사용자의 ID
     * @param friendId 삭제될 친구의 ID
     */
    @Transactional
    public void removeFriend(long removerId, long friendId) {
        friendshipRepository.delete(removerId, friendId);
    }

    /**
     * 두 사용자 간의 친구 관계 상태를 확인합니다.
     *
     * @param currentUserId 현재 사용자의 ID
     * @param otherUserId 상대방 사용자의 ID
     * @return 친구 관계 상태를 담은 Map (예: "SELF", "FRIENDS", "PENDING_SENT", "PENDING_RECEIVED", "NONE")
     */
    public Map<String, String> getFriendshipStatus(long currentUserId, long otherUserId) {
        if (currentUserId == otherUserId) {
            return Map.of("status", "SELF");
        }
        Optional<Friendship> friendshipOpt = friendshipRepository.findByUsers(currentUserId, otherUserId);
        String status = friendshipOpt.map(f -> {
            if (f.getStatus().name().equals("ACCEPTED")) {
                return "FRIENDS";
            }
            if (f.getStatus().name().equals("PENDING")) {
                return f.getUserId1() == currentUserId ? "PENDING_SENT" : "PENDING_RECEIVED";
            }
            return "NONE";
        }).orElse("NONE");

        return Map.of("status", status);
    }
}
