package com.example.simplechat.service;

import com.example.simplechat.dto.ChatRoomListDto;
import com.example.simplechat.dto.ChatRoomUserDto;
import com.example.simplechat.dto.RoomCreateDto;
import com.example.simplechat.dto.RoomInitDataDto;
import com.example.simplechat.dto.UserEventDto;
import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.exception.RegistrationException;
import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.ChatRoom;
import com.example.simplechat.model.User;
import com.example.simplechat.repository.MessageRepository;
import com.example.simplechat.repository.RoomRepository;
import com.example.simplechat.repository.RoomUserRepository;
import com.example.simplechat.repository.UserRepository;
import com.example.simplechat.service.LinkPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserRepository userRepository;
    private final MessageRepository msgRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomSessionManager roomSessionManager;
    private final LinkPreviewService linkPreviewService;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;


    public RoomInitDataDto initRoom(Long roomId, Long userId, int lines) {
	    ChatRoom room = getRoomById(roomId);
		
		if( !roomUserRepository.exists(userId, roomId) ) {
	    	throw new RegistrationException("FORBIDDEN", "비정상적인 접근입니다.");
		}
		
	    List<ChatMessageDto> messageDtos = mapMessagesToDto(msgRepository.findTopNByRoomIdOrderById(roomId, null, lines, "DESC"));

        // 각 메시지에 대해 비동기적으로 미리보기 생성 요청
        messageDtos.forEach(dto -> {
            String url = linkPreviewService.findFirstUrl(dto.content());
            if (url != null) {
                linkPreviewService.generateAndSendPreview(dto.messageId(), roomId, url);
            }
        });

     // 1. Repository에서 사용자 목록을 일단 가져옵니다.
        List<ChatRoomUserDto> rawUsers = findUsersByRoomId(roomId);

        // 2. 스트림을 사용해 각 사용자의 이미지 URL을 확인하고, null이면 기본 이미지 경로를 넣어줍니다.
        List<ChatRoomUserDto> correctedUsers = rawUsers.stream()
            .map(u -> {
                String imageUrl = u.profileImageUrl();
                if (imageUrl == null || imageUrl.isBlank() || imageUrl.endsWith("null")) { // DB 값이 null인 경우
                    imageUrl = profileStaticUrlPrefix + "/default.png";
                } else if (!imageUrl.startsWith(profileStaticUrlPrefix)) { // 상대 경로가 아닌 경우 (혹시 모를 예외 처리)
                    imageUrl = profileStaticUrlPrefix + "/" + imageUrl;
                }
                // DTO의 다른 필드는 그대로 두고, 이미지 URL만 수정한 새 DTO를 생성합니다.
                // (주의: ChatRoomUserDto에 모든 필드를 받는 생성자가 필요합니다.)
                return new ChatRoomUserDto(u.userId(), u.nickname(), u.role(), u.conn(), imageUrl);
            })
            .collect(Collectors.toList());

        // 3. 수정된 사용자 목록(correctedUsers)으로 최종 DTO를 생성하여 반환합니다.
        return new RoomInitDataDto(correctedUsers, messageDtos, room.getName());
	}

    private List<ChatMessageDto> mapMessagesToDto(List<ChatMessage> messages) {
        return messages.stream()
                .map(msg -> {
                    String profileImageUrl = userRepository.findProfileById(msg.getAuthor_id())
                            .map(profileData -> (String) profileData.get("profile_image_url"))
                            .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url : profileStaticUrlPrefix + "/default.png")
                            .orElse(profileStaticUrlPrefix + "/default.png");

                    return new ChatMessageDto(msg, profileImageUrl);
                })
                .collect(Collectors.toList());
    }


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

    @Transactional
    public Long createRoom(RoomCreateDto roomcreateDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        Long roomId = roomRepository.save(new ChatRoom(roomcreateDto.roomName(),
                roomcreateDto.roomType(), userId,
                roomcreateDto.password() != null ? passwordEncoder.encode(roomcreateDto.password()) : roomcreateDto.password())).getId();

        // add this user as ADMIN
        roomUserRepository.save(userId, roomId, user.getNickname(), "ADMIN");
        return roomId;
    }

    @Transactional
    public Long enterRoom(Long roomId, Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

        if (roomUserRepository.exists(userId, roomId)) {    // no need enter process
            System.out.println("User " + user.getNickname() + " is already in room " + room.getName());
            return roomId;
        }
        System.out.println("Password: " + room.getPassword_hash());
        //비밀번호 검증
        if (room.getRoom_type() == ChatRoom.RoomType.PRIVATE) {
            if (password == null || !passwordEncoder.matches(password, room.getPassword_hash())) {
                throw new RegistrationException("FORBIDDEN", "비밀번호가 일치하지 않습니다.");
            }
        }

        // 2. save 메서드에 초기 닉네임을 함께 전달합니다.
        roomUserRepository.save(userId, roomId, user.getNickname(), "MEMBER");

        return roomId;
    }

    @Transactional
    public void exitRoom(Long roomId, Long userId) {
        // 방장이 방을 나가려고 하는지 확인하는 로직
        String userRole = roomUserRepository.getRole(userId, roomId);
        if ("ADMIN".equals(userRole)) {
            throw new RegistrationException("FORBIDDEN", "방 개설자는 방을 나갈 수 없습니다. 방을 삭제해주세요.");
        }

        if (!roomUserRepository.exists(userId, roomId)) {
            System.out.println("User " + userId + " is not in room " + roomId);
            return;
        }

        roomUserRepository.delete(userId, roomId);
        eventPublisher.publishEvent(new UserExitedRoomEvent(this, userId, roomId, UserEventDto.EventType.ROOM_OUT));
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        // 1. 권한 검증
        String userRole = roomUserRepository.getRole(userId, roomId);
        if (!"ADMIN".equals(userRole)) {
            throw new RegistrationException("FORBIDDEN", "방을 삭제할 권한이 없습니다.");
        }

        // 2. 접속자에게 방 삭제 이벤트 발행
        eventPublisher.publishEvent(new UserExitedRoomEvent(this, null, roomId, UserEventDto.EventType.ROOM_DELETED));

        // 3. DB에서 관련 데이터 모두 삭제 (트랜잭션 보장)
        msgRepository.deleteByRoomId(roomId);
        roomUserRepository.deleteByRoomId(roomId);
        roomRepository.deleteById(roomId);

        System.out.println("Room " + roomId + " has been deleted by user " + userId);
    }

    public List<ChatRoomListDto> findRoomsByUserId(Long userId) {
        // 1. RoomUserRepository에서 사용자가 속한 방의 기본 정보를 가져옵니다.
        List<Map<String, Object>> roomInfos = roomUserRepository.findRoomsByUserId(userId);

        // 2. 각 방의 정보를 완전한 ChatRoomListDto로 변환합니다.
        return roomInfos.stream()
                .map(row -> {
                    Long roomId = (Long) row.get("room_id");

                    // 각 방의 전체 인원수와 현재 접속 인원수를 계산합니다.
                    int totalUsers = roomRepository.countUsersByRoomId(roomId); // 이 메소드가 RoomRepository에 필요합니다.
                    int connectedUsers = roomSessionManager.getConnectedUsers(roomId).size();

                    return new ChatRoomListDto(
                            roomId,
                            (String) row.get("room_name"),
                            (String) row.get("room_type"), // Enum 변환 제거
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
                .orElseThrow(() -> new IllegalArgumentException("Room not found!"));
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

    @Transactional
    public void kickUser(Long roomId, Long kickerId, Long userIdToKick) {
        // 1. 권한 검증
        String kickerRole = roomUserRepository.getRole(kickerId, roomId);
        if (!"ADMIN".equals(kickerRole)) {
            throw new RegistrationException("FORBIDDEN", "사용자를 추방할 권한이 없습니다.");
        }

        String userToKickRole = roomUserRepository.getRole(userIdToKick, roomId);
        if ("ADMIN".equals(userToKickRole)) {
            throw new RegistrationException("FORBIDDEN", "관리자는 다른 관리자를 추방할 수 없습니다.");
        }

        // 2. DB에서 사용자 삭제
        roomUserRepository.delete(userIdToKick, roomId);

        // 3. 이벤트 발행
        eventPublisher.publishEvent(new UserExitedRoomEvent(this, userIdToKick, roomId, UserEventDto.EventType.ROOM_OUT));
        System.out.println("User " + userIdToKick + " has been kicked from room " + roomId + " by user " + kickerId);
    }
}
