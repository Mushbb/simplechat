package com.example.simplechat.service;

import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.simplechat.dto.NotificationDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.dto.RoomInitDataDto;
import com.example.simplechat.dto.UserEventDto;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.Notification;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.MessageRepository;
import com.example.simplechat.repository.RoomRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 관리를 위한 서비스 클래스입니다. 방 생성, 삭제, 입장 및 퇴장, 방 내부 사용자 관리 및 관련 데이터 조회를 포함합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomService.class);
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserRepository userRepository;
    private final MessageRepository msgRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomSessionManager roomSessionManager;
    private final LinkPreviewService linkPreviewService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;


    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;

    /**
     * 사용자를 위해 채팅방의 현재 상태(최근 메시지 및 사용자 목록 포함)를 제공하여 채팅방을 초기화합니다.
     *
     * @param roomId 초기화할 방의 ID
     * @param userId 방에 입장하는 사용자의 ID
     * @param lines 가져올 최근 메시지의 수
     * @return 사용자 목록, 최근 메시지 및 방 이름을 포함하는 {@link RoomInitDataDto}
     * @throws RegistrationException 사용자가 해당 방의 멤버가 아닌 경우
     */
    public RoomInitDataDto initRoom(Long roomId, Long userId, int lines) {
        ChatRoom room = getRoomById(roomId);

        if (!roomUserRepository.exists(userId, roomId)) {
            throw new RegistrationException("FORBIDDEN", "이 방의 멤버가 아닙니다.");
        }

        List<ChatMessageDto> messageDtos = mapMessagesToDto(
            msgRepository.findTopNByRoomIdOrderById(roomId, null, lines, "DESC"));

        // 메시지에서 발견된 URL에 대한 링크 미리보기를 비동기적으로 요청합니다.
        messageDtos.forEach(dto -> {
            String url = linkPreviewService.findFirstUrl(dto.content());
            if (url != null) {
                linkPreviewService.generateAndSendPreview(dto.messageId(), roomId, url);
            }
        });

        List<ChatRoomUserDto> correctedUsers = findUsersByRoomId(roomId).stream()
            .map(u -> {
                String imageUrl = u.profileImageUrl();
                if (imageUrl == null || imageUrl.isBlank() || imageUrl.endsWith("null")) {
                    imageUrl = profileStaticUrlPrefix + "/default.png";
                } else if (!imageUrl.startsWith(profileStaticUrlPrefix)) {
                    imageUrl = profileStaticUrlPrefix + "/" + imageUrl;
                }
                return new ChatRoomUserDto(u.userId(), u.nickname(), u.role(), u.conn(),
                    imageUrl);
            })
            .collect(Collectors.toList());

        return new RoomInitDataDto(correctedUsers, messageDtos, room.getName());
    }

    private List<ChatMessageDto> mapMessagesToDto(List<ChatMessage> messages) {
        return messages.stream()
            .map(msg -> {
                String profileImageUrl = userRepository.findProfileById(msg.getAuthor_id())
                    .map(profileData -> (String) profileData.get("profile_image_url"))
                    .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url
                        : profileStaticUrlPrefix + "/default.png")
                    .orElse(profileStaticUrlPrefix + "/default.png");

                return new ChatMessageDto(msg, profileImageUrl);
            })
            .collect(Collectors.toList());
    }


    /**
     * 사용자 수와 같은 메타데이터를 포함하여 모든 채팅방 목록을 검색합니다.
     *
     * @param userId 현재 사용자의 ID. 각 방의 멤버인지 확인하는 데 사용됩니다.
     * @return {@link ChatRoomListDto} 객체 목록
     */
    public List<ChatRoomListDto> getRoomList(Long userId) {
        List<ChatRoomListDto> roomsFromDb = roomRepository.findAllWithCount();

        return roomsFromDb.stream()
            .map(roomDto -> new ChatRoomListDto(
                roomDto.id(),
                roomDto.name(),
                roomDto.roomType(),
                roomDto.ownerName(),
                roomDto.userCount(),
                roomSessionManager.getConnectedUsers(roomDto.id()).size(),
                roomUserRepository.exists(userId, roomDto.id())
            ))
            .collect(Collectors.toList());
    }

    /**
     * 새 채팅방을 만들고 생성자를 방의 관리자로 설정합니다.
     *
     * @param roomcreateDto 새 방의 세부 정보(이름, 유형, 비밀번호)를 포함하는 DTO
     * @param userId 방을 만드는 사용자의 ID
     * @return 새로 생성된 방의 ID
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public Long createRoom(RoomCreateDto roomcreateDto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다!"));

        ChatRoom newRoom = new ChatRoom(
            roomcreateDto.roomName(),
            roomcreateDto.roomType(),
            userId,
            roomcreateDto.password() != null ? passwordEncoder.encode(roomcreateDto.password())
                : null
        );
        Long roomId = roomRepository.save(newRoom).getId();

        roomUserRepository.save(userId, roomId, user.getNickname(), "ADMIN");
        return roomId;
    }

    /**
     * 사용자가 채팅방에 입장할 수 있게 합니다. 방이 비공개인 경우 비밀번호를 제공해야 합니다.
     *
     * @param roomId 입장할 방의 ID
     * @param userId 입장하는 사용자의 ID
     * @param password 방의 비밀번호 (비공개인 경우)
     * @return 방의 ID
     * @throws IllegalArgumentException 사용자 또는 방을 찾을 수 없는 경우
     * @throws RegistrationException 제공된 비밀번호가 비공개 방에 대해 올바르지 않은 경우
     */
    @Transactional
    public Long enterRoom(Long roomId, Long userId, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다!"));
        ChatRoom room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다!"));

        if (roomUserRepository.exists(userId, roomId)) {
            logger.info("사용자 {}는 이미 방 {}에 있습니다.", user.getNickname(), room.getName());
            return roomId;
        }

        if (room.getRoom_type() == ChatRoom.RoomType.PRIVATE) {
            if (password == null || !passwordEncoder.matches(password, room.getPassword_hash())) {
                throw new RegistrationException("FORBIDDEN", "비밀번호가 틀렸습니다.");
            }
        }

        roomUserRepository.save(userId, roomId, user.getNickname(), "MEMBER");
        return roomId;
    }

    /**
     * 사용자가 채팅방에서 나갈 수 있게 합니다. {@link UserExitedRoomEvent}를 발행합니다.
     *
     * @param roomId 나갈 방의 ID
     * @param userId 나가는 사용자의 ID
     * @throws RegistrationException 방 소유자가 방을 삭제하는 대신 나가려고 하는 경우
     */
    @Transactional
    public void exitRoom(Long roomId, Long userId) {
        String userRole = roomUserRepository.getRole(userId, roomId);
        if ("ADMIN".equals(userRole)) {
            throw new RegistrationException("FORBIDDEN", "방 소유자는 나갈 수 없습니다. 대신 방을 삭제해주세요.");
        }

        if (!roomUserRepository.exists(userId, roomId)) {
            logger.warn("사용자 {}가 방 {}에 없으므로 나갈 수 없습니다.", userId, roomId);
            return;
        }

        roomUserRepository.delete(userId, roomId);
        eventPublisher.publishEvent(
            new UserExitedRoomEvent(this, userId, roomId, UserEventDto.EventType.ROOM_OUT));
    }

    /**
     * 채팅방과 모든 관련 데이터를 삭제합니다. 방 소유자만 이 작업을 수행할 수 있습니다. 연결된 사용자에게 알리기 위해
     * {@link UserExitedRoomEvent}를 발행합니다.
     *
     * @param roomId 삭제할 방의 ID
     * @param userId 방을 삭제하려는 사용자의 ID
     * @throws RegistrationException 사용자가 방 소유자가 아닌 경우
     */
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        String userRole = roomUserRepository.getRole(userId, roomId);
        if (!"ADMIN".equals(userRole)) {
            throw new RegistrationException("FORBIDDEN", "이 방을 삭제할 권한이 없습니다.");
        }

        eventPublisher.publishEvent(
            new UserExitedRoomEvent(this, null, roomId, UserEventDto.EventType.ROOM_DELETED));

        msgRepository.deleteByRoomId(roomId);
        roomUserRepository.deleteByRoomId(roomId);
        roomRepository.deleteById(roomId);

        logger.info("방 {}이(가) 사용자 {}에 의해 삭제되었습니다.", roomId, userId);
    }

    /**
     * 특정 사용자가 참여한 모든 방을 찾습니다.
     *
     * @param userId 사용자의 ID
     * @return 사용자가 있는 방에 대한 {@link ChatRoomListDto} 객체 목록
     */
    public List<ChatRoomListDto> findRoomsByUserId(Long userId) {
        List<Map<String, Object>> roomInfos = roomUserRepository.findRoomsByUserId(userId);

        return roomInfos.stream()
            .map(row -> {
                Long roomId = (Long) row.get("room_id");
                int totalUsers = roomRepository.countUsersByRoomId(roomId);
                int connectedUsers = roomSessionManager.getConnectedUsers(roomId).size();

                return new ChatRoomListDto(
                    roomId,
                    (String) row.get("room_name"),
                    (String) row.get("room_type"),
                    (String) row.get("owner_name"),
                    totalUsers,
                    connectedUsers,
                    true
                );
            })
            .collect(Collectors.toList());
    }

    public ChatRoom getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다!"));
    }

    public List<ChatRoom> findRoomsByOwnerId(Long ownerId) {
        return roomRepository.findByOwnerId(ownerId);
    }

    public List<ChatRoomUserDto> findUsersByRoomId(Long roomId) {
        return roomRepository.findUsersByRoomId(roomId);
    }

    public List<ChatRoom> findAll() {
        return roomRepository.findAll();
    }

    public Optional<ChatRoom> findByName(String name) {
        return roomRepository.findByName(name);
    }

    /**
     * 채팅방에서 사용자를 강제 퇴장시킵니다. 방 소유자만 이 작업을 수행할 수 있습니다. {@link UserExitedRoomEvent}를
     * 발행합니다.
     *
     * @param roomId 방의 ID
     * @param kickerId 강제 퇴장을 수행하는 사용자의 ID (관리자여야 함)
     * @param userIdToKick 강제 퇴장될 사용자의 ID
     * @throws RegistrationException 강제 퇴장시키는 사람이 관리자가 아니거나 다른 관리자를 강제 퇴장시키려고 하는 경우
     */
    @Transactional
    public void kickUser(Long roomId, Long kickerId, Long userIdToKick) {
        String kickerRole = roomUserRepository.getRole(kickerId, roomId);
        if (!"ADMIN".equals(kickerRole)) {
            throw new RegistrationException("FORBIDDEN", "사용자를 강제 퇴장시킬 권한이 없습니다.");
        }

        String userToKickRole = roomUserRepository.getRole(userIdToKick, roomId);
        if ("ADMIN".equals(userToKickRole)) {
            throw new RegistrationException("FORBIDDEN", "다른 관리자를 강제 퇴장시킬 수 없습니다.");
        }

        roomUserRepository.delete(userIdToKick, roomId);

        eventPublisher.publishEvent(new UserExitedRoomEvent(this, userIdToKick, roomId,
            UserEventDto.EventType.ROOM_OUT));
        logger.info("사용자 {}이(가) 방 {}에서 사용자 {}에 의해 강제 퇴장되었습니다.", userIdToKick, roomId, kickerId);
    }

    /**
     * 채팅방에 참여하도록 사용자를 초대합니다. 초대받은 사람에게 알림을 생성합니다.
     *
     * @param roomId 사용자를 초대할 방의 ID
     * @param inviterId 초대를 보내는 사용자의 ID
     * @param inviteeId 초대받는 사용자의 ID
     * @throws RegistrationException 관련 엔티티(초대자, 초대받은 사람, 방)를 찾을 수 없거나, 초대자가 방에 없거나,
     * 초대받은 사람이 이미 방에 있는 경우
     */
    @Transactional
    public void inviteUserToRoom(Long roomId, Long inviterId, Long inviteeId) {
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "초대자를 찾을 수 없습니다."));
        User invitee = userRepository.findById(inviteeId)
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "초대받은 사람을 찾을 수 없습니다."));
        ChatRoom room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RegistrationException("NOT_FOUND", "방을 찾을 수 없습니다."));

        if (!roomUserRepository.exists(inviterId, roomId)) {
            throw new RegistrationException("FORBIDDEN", "초대할 권한이 없습니다.");
        }
        if (roomUserRepository.exists(inviteeId, roomId)) {
            throw new RegistrationException("CONFLICT", "사용자가 이미 방에 있습니다.");
        }

        String content =
            inviter.getNickname() + "님이 '" + room.getName() + "' 방으로 당신을 초대했습니다.";

        String metadata;
        try {
            metadata = objectMapper.writeValueAsString(Map.of(
                "inviterId", inviter.getId(),
                "inviterNickname", inviter.getNickname(),
                "roomId", room.getId(),
                "roomName", room.getName()
            ));
        } catch (Exception e) {
            throw new RuntimeException("메타데이터 직렬화에 실패했습니다.", e);
        }

        Notification notification = new Notification(inviteeId,
            Notification.NotificationType.ROOM_INVITATION, content, roomId, metadata);
        notificationService.save(notification);

        messagingTemplate.convertAndSendToUser(
            invitee.getUsername(),
            "/queue/notifications",
            NotificationDto.from(notification)
        );
    }
}

