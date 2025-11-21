package com.example.simplechat.service;

import com.example.simplechat.dto.FriendResponseDto;
import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.Friendship;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.FriendshipRepository;
import com.example.simplechat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Transactional
    public void sendFriendRequest(long senderId, long receiverId) {
        if (senderId == receiverId) {
            throw new RegistrationException("BAD_REQUEST", "You cannot send a friend request to yourself.");
        }
        // Check if users exist
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "Sender not found."));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "Receiver not found."));

        // Check if a friendship already exists
        friendshipRepository.findByUsers(senderId, receiverId).ifPresent(f -> {
            throw new RegistrationException("CONFLICT", "Friendship or request already exists.");
        });

        // Friendship 테이블 대신 Notification 테이블에 저장
        String content = sender.getNickname() + "님이 친구 요청을 보냈습니다.";
        Notification notification = new Notification(receiverId, Notification.NotificationType.FRIEND_REQUEST, content, senderId, null);
        notificationService.save(notification);

        // 실시간 알림 전송 (새 DTO 사용)
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/notifications",
                NotificationDto.from(notification)
        );
    }

    public List<FriendResponseDto> getFriends(long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, Friendship.Status.ACCEPTED);

        return friendships.stream()
                .map(friendship -> {
                    long friendId = friendship.getUserId1() == userId ? friendship.getUserId2() : friendship.getUserId1();
                    User friend = userRepository.findById(friendId)
                            .orElseThrow(() -> new IllegalStateException("Friend user not found"));
                    String url = friend.getProfile_image_url();
                    friend.setProfile_image_url(url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url : profileStaticUrlPrefix + "/default.png");
                    FriendResponseDto.ConnectType conn = presenceService.isUserOnline(friendId) ? FriendResponseDto.ConnectType.CONNECT : FriendResponseDto.ConnectType.DISCONNECT;

                    return FriendResponseDto.from(friend, "ACCEPTED", conn);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFriend(long removerId, long friendId) {
        friendshipRepository.delete(removerId, friendId);
    }

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
