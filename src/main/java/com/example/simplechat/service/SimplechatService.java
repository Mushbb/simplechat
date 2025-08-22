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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Scanner;
import jakarta.annotation.PreDestroy;
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
    private final Scanner sc = new Scanner(System.in);
	
    private final ApplicationEventPublisher eventPublisher; // 주입
    private final PresenceService presenceService;
    
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository msgRepository;
    private final RoomUserRepository roomUserRepository;
    private final FriendshipRepository friendshipRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomSessionManager roomSessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LinkPreviewService linkPreviewService; // LinkPreviewService 주입

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
	
    @PostConstruct
    public void init() {
        System.out.println("Initializing SimplechatService...");

        // 시스템 유저를 찾고, 만약 없다면 새로 생성해서 저장한 뒤, 그 결과를 사용한다.
        this.systemUser = userRepository.findById(0L).orElseGet(() -> {
            System.out.println("System user not found. Creating a new one...");
            User newUser = new User(0L, "system");
            newUser.setNickname("시스템");
            newUser.setPassword_hash("");
            return userRepository.insertwithId(newUser);
        });

        System.out.println("System user '" + this.systemUser.getNickname() + "' cached successfully.");
    }
    
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		String input = sc.nextLine();
		ChatMessage newMsg;
		
		if( input.startsWith("/") ) {
			newMsg = ServerCommand(input);
			if( newMsg == null )
				return;
		} else {
			if( serverChat_room == null ) {
				System.out.println("There is no selected room.");
				return;
			}
			// normal msg to selected room
			newMsg = ServerMessage(input);
		}
		
		newMsg = msgRepository.save(newMsg);
		eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, newMsg, newMsg.getRoom_id()));
        System.out.println("ChatRoom[" + newMsg.getRoom_id() + "]: ChatMessageAddedToRoomEvent 발행됨.");
	}
	
	private ChatMessage ServerMessage(String input) {
		ChatMessage newMsg = new ChatMessage( systemUser.getId(), serverChat_room.getId(), "시스템");
		newMsg.setContent(input);
		newMsg.setMsg_type(ChatMessage.MsgType.TEXT);
		
		return newMsg;
	}
	
	private ChatMessage ServerCommand(String command) {
		CommandParser.ParseResult result = CommandParser.parse(command);
		
		switch (result.command) {
		// /rooms -> 전체 방 목록 확인
		case "rooms":
			List<ChatRoom> allRooms = roomRepository.findAll();
			if(allRooms.isEmpty()) {
				System.out.println("No chat rooms found.");
			} else {
				System.out.println("--- Chat Room List ---");
				System.out.printf("%-10s | %-20s | %-10s | %-15s%n", "ID", "Room Name", "Type", "Created At");
				for(ChatRoom room : allRooms) {
					System.out.printf("%-10s | %-20s | %-10s | %-15s\n", 
							room.getId(),
							room.getName(),
							room.getRoom_type().name(),
							room.getCreated_at());
					System.out.println("------------------------------------------------------------");
				}
			}
			return null;
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
			return null;
		// /enter <roomName> -> 특정 방을 타겟으로 지정
		case "enter":
			Optional<ChatRoom> room = roomRepository.findByName(result.args);
			if( !room.isEmpty() ) {
				serverChat_room = room.get();
			} else {
				System.out.println("There is no such room.");
			}
			return null;
		// /users -> 해당 방의 현재 사용자 목록 확인
		case "users":
			if( serverChat_room == null )
				System.out.println("There is no selected room.");
			
			List<ChatRoomUserDto> allUsers = roomRepository.findUsersByRoomId(serverChat_room.getId());
			if( allUsers.isEmpty() ) {
				System.out.println("There is no users.");
			} else {
				System.out.println("--- User List ---");
				System.out.printf("%-10s | %-20s | %-10s | %-15s%n", "ID", "Nickname", "Role");
				for(ChatRoomUserDto user : allUsers) {
					System.out.printf("%-10s | %-20s | %-10s\n", 
							user.userId(),
							user.nickname(),
							user.role());
					System.out.println("------------------------------------------------------------");
				}
			}
			return null;
		default:
			System.out.println("Unknown command: " + result.command);
			return null;
		}
	}
	
	@Transactional
	public User register(UserRegistrationRequestDto requestDto) {
		if(requestDto.username().equals("system")) {	// invalid username
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
		User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("User not found!"));
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
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("User not found!"));
	    ChatRoom room = roomRepository.findById(roomId)
	            .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

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

		Friendship newRequest = new Friendship(senderId, receiverId, Friendship.Status.PENDING, null, 0);
		friendshipRepository.save(newRequest);

		// Send real-time notification
		FriendResponseDto responseDto = FriendResponseDto.from(sender, "PENDING_RECEIVED", null);
		NotificationDto<FriendResponseDto> notification = new NotificationDto<>("FRIEND_REQUEST", responseDto);

		messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/notifications", notification);
	}

	public List<FriendResponseDto> getPendingRequests(long userId) {
		List<Friendship> requests = friendshipRepository.findIncomingPendingRequests(userId);
		
		return requests.stream()
				.map(friendship -> {
					User requester = userRepository.findById(friendship.getUserId1())
							.orElseThrow(() -> new IllegalStateException("Requester not found"));
					return FriendResponseDto.from(requester, "PENDING_RECEIVED", null);
				})
				.collect(Collectors.toList());
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
					System.out.println(conn.name());
					return FriendResponseDto.from(friend, "ACCEPTED", conn);
				})
				.collect(Collectors.toList());
	}

	@Transactional
	public void acceptFriendRequest(long accepterId, long requesterId) {
		Friendship friendship = friendshipRepository.findByUsers(accepterId, requesterId)
				.filter(f -> f.getStatus().name().equals("PENDING") && f.getUserId2() == accepterId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "No pending request found to accept."));
		
		friendshipRepository.updateStatus(requesterId, accepterId, "ACCEPTED");

		// Optionally, send a notification back to the requester
		User accepter = userRepository.findById(accepterId).orElseThrow(() -> new IllegalStateException("Accepter not found"));
		NotificationDto<FriendResponseDto> notification = new NotificationDto<>("FRIEND_ACCEPTED", FriendResponseDto.from(accepter, "ACCEPTED", null));
		String requesterUsername = userRepository.findById(requesterId).get().getUsername();
		messagingTemplate.convertAndSendToUser(requesterUsername, "/queue/notifications", notification);
	}

	@Transactional
	public void rejectFriendRequest(long rejecterId, long requesterId) {
		Friendship friendship = friendshipRepository.findByUsers(rejecterId, requesterId)
				.filter(f -> f.getStatus().name().equals("PENDING") && f.getUserId2() == rejecterId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "No pending request found to reject."));
		
		friendshipRepository.deleteByRequesterAndReceiver(requesterId, rejecterId);
	}

	@Transactional
	public void removeFriend(long removerId, long friendId) {
		Friendship friendship = friendshipRepository.findByUsers(removerId, friendId)
				.filter(f -> f.getStatus().name().equals("ACCEPTED"))
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "Friendship not found."));

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
			return "NONE"; // Should not happen if status is handled correctly
		}).orElse("NONE");

		return Map.of("status", status);
	}
	
	@Transactional
    public void inviteUserToRoom(Long roomId, Long inviterId, Long inviteeId) {
        // 1. 초대하는 사람(inviter)이 해당 방에 있는지 먼저 확인 (권한 체크)
        if (!roomUserRepository.exists(inviterId, roomId)) {
            throw new RegistrationException("FORBIDDEN", "초대 권한이 없습니다.");
        }

        // 2. 초대받는 사람(invitee)이 이미 방에 있는지 확인
        if (roomUserRepository.exists(inviteeId, roomId)) {
            throw new RegistrationException("CONFLICT", "이미 참여하고 있는 사용자입니다.");
        }

        // 3. 초대받는 사람의 User 객체를 가져옵니다 (닉네임 등이 필요)
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new RegistrationException("NOT_FOUND", "초대할 사용자를 찾을 수 없습니다."));

        // 4. 사용자를 방에 추가합니다 ('MEMBER' 역할로).
        roomUserRepository.save(inviteeId, roomId, invitee.getNickname(), "MEMBER");

        // 5. 방에 있는 모든 사용자에게 새로운 멤버가 입장했음을 알립니다.
        //    (기존의 입장/퇴장 이벤트 시스템을 재활용)
        eventPublisher.publishEvent(new UserEnteredRoomEvent(this, invitee, roomId, UserEventDto.UserType.MEMBER));
        
     // 1. 초대된 방의 상세 정보를 조회합니다.
        ChatRoomListDto roomDto = roomRepository.findRoomDtoById(roomId)
                .orElseThrow(() -> new IllegalStateException("Room DTO not found after creation"));

        // 2. "FORCED_JOIN" 타입의 특별 알림을 생성합니다.
        NotificationDto<ChatRoomListDto> notification = new NotificationDto<>("FORCED_JOIN", roomDto);

        // 3. 초대받은 사용자(invitee)에게만 개인 큐로 알림을 보냅니다.
        messagingTemplate.convertAndSendToUser(invitee.getUsername(), "/queue/notifications", notification);
    }
	
	@PreDestroy
	public void closeScanner() { sc.close(); }
	
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