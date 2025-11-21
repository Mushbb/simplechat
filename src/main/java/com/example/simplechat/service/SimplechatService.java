package com.example.simplechat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import com.example.simplechat.repository.*;
import com.example.simplechat.model.*;
import com.example.simplechat.dto.*;
import com.example.simplechat.event.*;
import com.example.simplechat.exception.*;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class SimplechatService {
    private ChatRoom serverChat_room = null;
    private User systemUser;
//  private final Scanner sc = new Scanner(System.in);	// 서버 콘솔 사용불가
	
    private final ApplicationEventPublisher eventPublisher;
    private final PresenceService presenceService;
    
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository msgRepository;
    private final RoomUserRepository roomUserRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    
    private final PasswordEncoder passwordEncoder;
    private final RoomSessionManager roomSessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LinkPreviewService linkPreviewService;

    @Autowired
    @Qualifier("profileFileRepository")
    private FileRepository profileFileRepository;

    @Autowired
    @Qualifier("chatFileRepository")
    private FileRepository chatFileRepository;

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;
    @Value("${file.chat-static-url-prefix}")
    private String chatStaticUrlPrefix;

    @Value("${file.profile-upload-dir}")
    private String profileUploadDir;

    @Value("${file.chat-upload-dir}")
    private String chatUploadDir;

    @Value("${file.retention-days}")
    private long retentionDays;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void cleanupOldFiles() {
        System.out.println("Starting scheduled cleanup of old files...");
        long retentionMillis = retentionDays * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - retentionMillis;

        // deleteOldFiles(profileUploadDir, cutoffTime); // 프로필 사진은 삭제 대상에서 제외
        deleteOldFiles(chatUploadDir, cutoffTime);

        System.out.println("Scheduled cleanup of old files finished.");
    }

    private void deleteOldFiles(String directoryPath, long cutoffTime) {
        try {
            Path directory = Paths.get(directoryPath);
            if (Files.exists(directory) && Files.isDirectory(directory)) {
                Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (Files.getLastModifiedTime(file).toMillis() < cutoffTime) {
                                Files.delete(file);
                                System.out.println("Deleted old file: " + file);
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to delete file: " + file + " - " + e.getMessage());
                        }
                    });
            } else {
                System.out.println("Directory not found, skipping cleanup: " + directoryPath);
            }
        } catch (IOException e) {
            System.err.println("Error during file cleanup in directory: " + directoryPath + " - " + e.getMessage());
        }
    }

	
    @PostConstruct
    public void init() {
        System.out.println("Initializing SimplechatService...");

        // 시스템 유저를 찾고, 만약 없다면 새로 생성해서 저장한 뒤, 그 결과를 사용한다.
        this.systemUser = userRepository.findById(0L).orElseGet(() -> {
            System.out.println("System user not found. Creating a new one...");
            User newUser = new User(0L, "system");
            newUser.setNickname("시스템");
            newUser.setPassword_hash(passwordEncoder.encode("sysadmin"));
            return userRepository.insertwithId(newUser);
        });

        System.out.println("System user '" + this.systemUser.getNickname() + "' cached successfully.");
    }
    
//	public void serverChat() {
//		String input = sc.nextLine();
//		ChatMessage newMsg;
//		
//		if( input.startsWith("/") ) {
//			newMsg = ServerCommand(input);
//			if( newMsg == null )
//				return;
//		} else {
//			if( serverChat_room == null ) {
//				System.out.println("There is no selected room.");
//				return;
//			}
//			newMsg = ServerMessage(input);
//		}
//		
//		newMsg = msgRepository.save(newMsg);
//		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, newMsg, newMsg.getRoom_id()));
//        System.out.println("ChatRoom[" + newMsg.getRoom_id() + "]: ChatMessageAddedToRoomEvent 발행됨.");
//	}
//	
//	private ChatMessage ServerMessage(String input) {
//		ChatMessage newMsg = new ChatMessage( systemUser.getId(), serverChat_room.getId(), "시스템");
//		newMsg.setContent(input);
//		newMsg.setMsg_type(ChatMessage.MsgType.TEXT);
//		
//		return newMsg;
//	}
//	
//	private ChatMessage ServerCommand(String command) {
//		String result = executeAdminCommand(command);
//		System.out.println(result);
//		return null; // ServerCommand no longer creates messages directly
//	}

	public String executeAdminCommand(String command) {
		CommandParser.ParseResult result = CommandParser.parse(command);
		
		switch (result.command) {
		// /rooms -> 전체 방 목록 확인
		case "rooms":
			List<ChatRoom> allRooms = roomRepository.findAll();
			if(allRooms.isEmpty()) {
				return "No chat rooms found.";
			} else {
				StringBuilder sb = new StringBuilder("--- Chat Room List ---\n");
				sb.append(String.format("%-10s | %-20s | %-10s | %-15s%n", "ID", "Room Name", "Type", "Created At"));
				for(ChatRoom room : allRooms) {
					sb.append(String.format("%-10s | %-20s | %-10s | %-15s\n", 
							room.getId(),
							room.getName(),
							room.getRoom_type().name(),
							room.getCreated_at()));
					sb.append("------------------------------------------------------------\n");
				}
				return sb.toString();
			}
		// /create <roomName> [options] -> 새로운 방 생성
		// options: -public(default), -private <password>, -game <gametype>
		case "create":
			ChatRoom.RoomType room_type;
			String pass_hash = null;
			if( result.options.containsKey("private") ) {
				room_type = ChatRoom.RoomType.PRIVATE;
				pass_hash = result.options.get("private");
			} else {
				room_type = ChatRoom.RoomType.PUBLIC;
			}
			
			createRoom(new RoomCreateDto(result.args, room_type, pass_hash), 0L);
			return "Room '" + result.args + "' created.";
		// /enter <roomName> -> 특정 방을 타겟으로 지정
		case "enter":
			Optional<ChatRoom> room = roomRepository.findByName(result.args);
			if( !room.isEmpty() ) {
				serverChat_room = room.get();
				return "Entered room: " + room.get().getName();
			} else {
				return "There is no such room.";
			}
		// /users -> 해당 방의 현재 사용자 목록 확인
		case "users":
			if( serverChat_room == null )
				return "There is no selected room.";
			
			List<ChatRoomUserDto> allUsers = roomRepository.findUsersByRoomId(serverChat_room.getId());
			if( allUsers.isEmpty() ) {
				return "There are no users in this room.";
			} else {
				StringBuilder sb = new StringBuilder("--- User List ---\n");
				sb.append(String.format("%-10s | %-20s | %-10s%n", "ID", "Nickname", "Role"));
				for(ChatRoomUserDto user : allUsers) {
					sb.append(String.format("%-10s | %-20s | %-10s\n", 
							user.userId(),
							user.nickname(),
							user.role()));
					sb.append("------------------------------------------------------------\n");
				}
				return sb.toString();
			}
		// /cleanup -> 수동으로 오래된 파일 정리 실행
		case "cleanup":
			cleanupOldFiles();
			return "Manual cleanup finished.";
		default:
			return "Unknown command: " + result.command;
		}
	}
	
	// 신규 사용자 등록
	@Transactional
	public User register(UserRegistrationRequestDto requestDto) {
		if(requestDto.username().equals("system")) {
			throw new RegistrationException("INVALID_USERNAME","Invalid Username");
		} else if( !userRepository.findByUsername(requestDto.username()).isEmpty() ) {	
			throw new RegistrationException("DUPLICATE_USERNAME","Username is already exist");
		}
		
		return userRepository.save(new User(requestDto.username(), passwordEncoder.encode(requestDto.password()), requestDto.nickname()));
	}
	
	public User login(LoginRequestDto requestDto) {
		Optional<User> requested = userRepository.findByUsername(requestDto.username());
		if( requested.isEmpty() ) {
			throw new RegistrationException("INVALID_USERNAME","Username is not exist");
		} else if( !passwordEncoder.matches( requestDto.password(), requested.get().getPassword_hash() ) ) {
			throw new RegistrationException("INVALID_PASSWORD","Password is wrong");
		}
		
		return requested.get();
	}
	
	public User getUserById(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));
	}
	@Transactional
	public Long delete_account(Long userId) {
		if(!userRepository.existsById(userId)) {
			throw new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
		}

		// 3. 방장이었던 방도 같이 삭제
		for( ChatRoom room : roomRepository.findByOwnerId(userId) ) {
			deleteRoom(room.getId(), userId);
		}
		
		// 1. 사용자가 속한 모든 채팅방 멤버십 정보 삭제
		roomUserRepository.deleteByUserId(userId);
		
		// 2. 사용자 계정 자체를 삭제
		userRepository.deleteById(userId);

		return userId;
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
	
	public List<ChatRoomListDto> getRoomList(Long userId){
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
				roomcreateDto.password()!=null?passwordEncoder.encode(roomcreateDto.password()):roomcreateDto.password() ) ).getId();
		
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
	    
	    if( roomUserRepository.exists(userId, roomId) ) {	// no need enter process
	    	System.out.println("User " + user.getNickname() + " is already in room " + room.getName());
			return roomId;
		}
	    System.out.println("Password: "+room.getPassword_hash());
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
	
	public RoomInitDataDto initRoom(Long roomId, Long userId, int lines) {
	    ChatRoom room = roomRepository.findById(roomId)
	            .orElseThrow(() -> new IllegalArgumentException("Room not found!"));
		
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
        List<ChatRoomUserDto> rawUsers = roomRepository.findUsersByRoomId(roomId);

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

	@Transactional
	public void changeNicknameInRoom(NickChangeDto nickChangeDto) {
	    // (필요하다면) 닉네임 유효성 검사 (길이, 중복 등) 로직 추가
		
		Long userId = nickChangeDto.userId();
		Long roomId = nickChangeDto.roomId();
		String newNickname = nickChangeDto.newNickname();

	    // 1. DB의 chat_room_users 테이블에 있는 닉네임을 업데이트
	    roomUserRepository.updateNickname(userId, roomId, newNickname);

	    // 2. 닉네임 변경 이벤트를 발행하여 다른 사용자들에게 알림
	    eventPublisher.publishEvent(new ChangeNicknameEvent(this, userId, roomId, newNickname));
	    System.out.println("User " + userId + "'s nickname in room " + roomId + " changed to " + newNickname);
	}

	public void addChat_publish(ChatMessageRequestDto msgDto) {
		String AuthorName = roomUserRepository.getNickname(msgDto.authorId(), msgDto.roomId());
		ChatMessage savedMessage = msgRepository.save(new ChatMessage(msgDto, AuthorName));
		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, savedMessage, msgDto.roomId()));

		// 멘션된 사용자들에게 알림 전송
		if (msgDto.mentionedUserIds() != null && !msgDto.mentionedUserIds().isEmpty()) {
			User author = userRepository.findById(msgDto.authorId())
					.orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지 작성자를 찾을 수 없습니다."));
			ChatRoom room = roomRepository.findById(msgDto.roomId())
					.orElseThrow(() -> new RegistrationException("NOT_FOUND", "채팅방을 찾을 수 없습니다."));

			for (Long mentionedUserId : msgDto.mentionedUserIds()) {
				User mentionedUser = userRepository.findById(mentionedUserId)
						.orElse(null); // 멘션된 사용자가 없으면 스킵

				if (mentionedUser != null && !mentionedUser.getId().equals(author.getId())) { // 자기 자신 멘션 제외
					String content = author.getNickname() + "님이 '" + room.getName() + "' 방에서 회원님을 멘션했습니다.";
					Notification notification = new Notification(
							mentionedUserId,
							Notification.NotificationType.MENTION,
							content,
							room.getId(), // relatedEntityId를 roomId로 사용
							null // metadata는 필요시 추가
					);
					notificationService.save(notification);

					// 실시간 알림 전송
					messagingTemplate.convertAndSendToUser(
							mentionedUser.getUsername(),
							"/queue/notifications",
							NotificationDto.from(notification)
					);
				}
			}
		}
	}

	@Transactional
	public void deleteMessage(Long messageId, Long userId) {
		// 1. 메시지 정보 조회
		ChatMessage message = msgRepository.findById(messageId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

		Long roomId = message.getRoom_id();

		// 2. 권한 검증
		String userRole = roomUserRepository.getRole(userId, roomId);
		boolean isAdmin = "ADMIN".equals(userRole);
		boolean isAuthor = message.getAuthor_id().equals(userId);

		if (!isAdmin && !isAuthor) {
			throw new RegistrationException("FORBIDDEN", "메시지를 삭제할 권한이 없습니다.");
		}

		// 3. DB에서 메시지 삭제
		msgRepository.deleteById(messageId);

		// 4. 삭제 이벤트 발행
		ChatMessage deleteEventMessage = new ChatMessage(messageId, roomId, ChatMessage.MsgType.DELETE);
		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, deleteEventMessage, roomId));

		System.out.println("Message " + messageId + " has been deleted by user " + userId);
	}

	@Transactional
	public void editMessage(Long messageId, Long userId, String newContent) {
		// 1. 메시지 정보 조회
		ChatMessage message = msgRepository.findById(messageId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "메시지를 찾을 수 없습니다."));

		Long roomId = message.getRoom_id();

		// 2. 권한 검증
		String userRole = roomUserRepository.getRole(userId, roomId);
		boolean isAdmin = "ADMIN".equals(userRole);
		boolean isAuthor = message.getAuthor_id().equals(userId);

		if (!isAdmin && !isAuthor) {
			throw new RegistrationException("FORBIDDEN", "메시지를 수정할 권한이 없습니다.");
		}

		// 3. 내용이 비어있거나, 기존 내용과 같으면 수정하지 않음
		if (newContent == null || newContent.isBlank() || newContent.equals(message.getContent())) {
			return;
		}

		// 4. DB 업데이트
		message.setContent(newContent);
		message.setMsg_type(ChatMessage.MsgType.UPDATE); // 이벤트 발행을 위해 타입 변경
		msgRepository.save(message);

		// 5. 수정 이벤트 발행
		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, message, roomId));

		System.out.println("Message " + messageId + " has been edited by user " + userId);
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
	
	public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
		List<ChatMessage> messages = msgRepository.findTopNByRoomIdOrderById(
                        msgListDto.roomId(),
                        msgListDto.beginId(),
                        msgListDto.rowCount(),
                        "DESC");

        List<ChatMessageDto> messageDtos = mapMessagesToDto(messages);

        // 각 메시지에 대해 비동기적으로 미리보기 생성 요청
        messageDtos.forEach(dto -> {
            String url = linkPreviewService.findFirstUrl(dto.content());
            if (url != null) {
                linkPreviewService.generateAndSendPreview(dto.messageId(), msgListDto.roomId(), url);
            }
        });

        return new ChatMessageListDto(msgListDto.roomId(), messageDtos.reversed());
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
	
	public UserProfileDto getUserProfile(Long userId) {
		// 1. UserRepository를 사용하여 프로필 정보 조회
		Map<String, Object> profileData =
				userRepository.findProfileById(userId)
						.orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

		String imageUrl = (String) profileData.get("profile_image_url");
		
		if (imageUrl == null || imageUrl.isBlank()) {
			imageUrl = profileStaticUrlPrefix + "/default.png"; // 기본 이미지 경로
		} else {
			imageUrl = profileStaticUrlPrefix + "/" + imageUrl; // 저장된 이미지 경로
		}
		
		// 2. Map에서 DTO로 데이터 변환
		return new UserProfileDto(
				(Long) profileData.get("user_id"),
				(String) profileData.get("username"),
				(String) profileData.get("nickname"),
				(String) profileData.get("status_message"),
				imageUrl
		);
	}
	@Transactional
	public UserProfileDto changeUserProfile(ProfileUpdateRequestDto profileDto, Long userId) {
		// 1. DB에서 현재 사용자 정보를 가져옴
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

		// 2. DTO의 값으로 User 객체의 필드를 업데이트
		user.setNickname(profileDto.nickname());
		user.setStatus_message(profileDto.statusMessage());

		// 3. save 메서드를 호출하여 변경된 필드만 동적으로 업데이트
		userRepository.save(user);

		// 4. 변경된 최신 프로필 정보를 다시 조회하여 반환
		return getUserProfile(userId);
	}
	@Transactional
	public String updateProfileImage(Long userId, MultipartFile file) {
		// 1. 사용자 정보 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

		// 2. 기존 프로필 이미지가 있으면 삭제
		String oldFilename = user.getProfile_image_url();
		if (oldFilename != null && !oldFilename.isBlank()) {
			profileFileRepository.delete(oldFilename);
		}

		// 3. 새 파일 저장
		String newFilename = profileFileRepository.save(file);

		// 4. DB에 새 파일명 업데이트
		user.setProfile_image_url(newFilename);
		userRepository.save(user);

		// 5. 클라이언트가 접근할 수 있는 URL 경로 반환
		return profileStaticUrlPrefix + "/" + newFilename;
	}

	public void uploadChatFile(Long roomId, Long userId, MultipartFile file) {
		// 1. 파일 저장
		String storedFilename = chatFileRepository.save(file);
		String originalFilename = file.getOriginalFilename();

		// 2. 파일 정보를 담은 메시지 생성 (원본명:저장명)
		//    나중에 클라이언트에서 a 태그로 만들 때 파싱해서 사용
		String fileInfoContent = originalFilename + ":" + storedFilename;

		// 3. 채팅 메시지 객체 생성
		String authorName = roomUserRepository.getNickname(userId, roomId);
		ChatMessage fileMessage = new ChatMessage(roomId, userId, authorName, fileInfoContent, ChatMessage.MsgType.FILE);

		// 4. 메시지 DB 저장 및 이벤트 발행
		ChatMessage savedMessage = msgRepository.save(fileMessage);
		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, savedMessage, roomId));
	}
	

	// =================================================================================================
	// Friendship Management
	// =================================================================================================

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
	
	    @Transactional
	    public void inviteUserToRoom(Long roomId, Long inviterId, Long inviteeId) {
			User inviter = userRepository.findById(inviterId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "Inviter not found."));
	        User invitee = userRepository.findById(inviteeId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "Invitee not found."));
	        ChatRoom room = roomRepository.findById(roomId).orElseThrow(() -> new RegistrationException("NOT_FOUND", "Room not found."));
	
	        if (!roomUserRepository.exists(inviterId, roomId)) {
	            throw new RegistrationException("FORBIDDEN", "초대 권한이 없습니다.");
	        }
	        if (roomUserRepository.exists(inviteeId, roomId)) {
	            throw new RegistrationException("CONFLICT", "이미 참여하고 있는 사용자입니다.");
	        }
	        
	        String content = inviter.getNickname() + "님이 '" + room.getName() + "' 방에 초대했습니다.";
	        
	        // metadata에 초대한 사람과 방 정보를 JSON 형태로 저장
	        String metadata;
	        try {
	            metadata = new ObjectMapper().writeValueAsString(Map.of(
	                "inviterId", inviter.getId(),
	                "inviterNickname", inviter.getNickname(),
	                "roomId", room.getId(),
	                "roomName", room.getName()
	            ));
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to serialize metadata", e);
	        }
	
	        Notification notification = new Notification(inviteeId, Notification.NotificationType.ROOM_INVITATION, content, roomId, metadata);
	        notificationService.save(notification);
	        
	        messagingTemplate.convertAndSendToUser(
	            invitee.getUsername(),
	            "/queue/notifications",
	            NotificationDto.from(notification)
	        );
	    }
	   
	//	@PreDestroy
	//	public void closeScanner() { sc.close(); }	
	///////////////////////////////////////////////
	// CommandLine Parser
	public class CommandParser {
	    public static class ParseResult {
	        public String command;
	        public String args; // 옵션 전까지 띄어쓰기 포함 인자
	        public Map<String, String> options = new HashMap<>();

	        @Override
	        public String toString() {
	            return "ParseResult{" +
	                    "command='" + command + '\'' +
	                    ", args='" + args + '\'' +
	                    ", options=" + options +
	                    '}';
	        }
	    }

	    public static ParseResult parse(String input) {
	        ParseResult result = new ParseResult();

	        if (input == null || input.trim().isEmpty()) {
	            throw new IllegalArgumentException("빈 입력입니다.");
	        }

	        String[] tokens = input.trim().split("\\s+");
	        if (!tokens[0].startsWith("/")) {
	            throw new IllegalArgumentException("명령어는 /로 시작해야 합니다.");
	        }

	        result.command = tokens[0].substring(1);

	        // 옵션(-) 시작 전까지 args 추출
	        StringBuilder argsBuilder = new StringBuilder();
	        int i = 1;
	        for (; i < tokens.length; i++) {
	            if (tokens[i].startsWith("-")) {
	                break;
	            }
	            if (argsBuilder.length() > 0) {
	                argsBuilder.append(" ");
	            }
	            argsBuilder.append(tokens[i]);
	        }
	        result.args = argsBuilder.toString();

	        // 옵션 파싱
	        while (i < tokens.length) {
	            String token = tokens[i];
	            if (!token.startsWith("-")) {
	                throw new IllegalArgumentException("옵션 이름은 -로 시작해야 합니다: " + token);
	            }
	            String optionName = token.substring(1);
	            String optionValue = "true"; // 값이 없으면 true 처리

	            if (i + 1 < tokens.length && !tokens[i + 1].startsWith("-")) {
	                optionValue = tokens[i + 1];
	                i++;
	            }
	            result.options.put(optionName, optionValue);
	            i++;
	        }

	        return result;
	    }
	}
}