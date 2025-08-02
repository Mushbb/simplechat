package com.example.simplechat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Scanner;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;

import com.example.simplechat.repository.*;

import lombok.RequiredArgsConstructor;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.User;
import com.example.simplechat.model.ChatRoom;

import com.example.simplechat.dto.*;
import com.example.simplechat.event.*;
import com.example.simplechat.exception.*;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor 
@EnableScheduling
public class SimplechatService {
    private ChatRoom serverChat_room = null;
    private User systemUser;
    private final Scanner sc = new Scanner(System.in);
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final ApplicationEventPublisher eventPublisher; // Spring의 ApplicationEventPublisher 주입
    
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository msgRepository;
    private final RoomUserRepository roomUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomSessionManager roomSessionManager;
    private final FileRepository fileRepository;

    @Value("${file.static-url-prefix}")
    private String staticUrlPrefix;
	
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
	
	public List<ChatRoomListDto> getRoomList(){
		List<ChatRoomListDto> roomsFromDb = roomRepository.findAllWithCount();

        return roomsFromDb.stream()
            .map(roomDto -> new ChatRoomListDto(
                roomDto.id(),
                roomDto.name(),
                roomDto.roomType(),
                roomDto.ownerName(),
                roomDto.userCount(),
                roomSessionManager.getConnectedUsers(roomDto.id()).size()
            ))
            .collect(Collectors.toList());
	}
	
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
		
	    //eventPublisher.publishEvent(new UserEnteredRoomEvent(this, user, roomId));
		
		return new RoomInitDataDto(roomRepository.findUsersByRoomId(roomId),
				msgRepository.findTopNByRoomIdOrderById(roomId, null , lines, "DESC").stream()
				.map(msg -> {
                    // 각 메시지의 작성자 프로필 이미지 URL을 조회합니다.
                    String profileImageUrl = userRepository.findProfileById(msg.getAuthor_id())
                            .map(profileData -> (String) profileData.get("profile_image_url"))
                            .map(url -> url != null && !url.isBlank() ? staticUrlPrefix + "/" + url : staticUrlPrefix + "/default.png")
                            .orElse(staticUrlPrefix + "/default.png");

                    // 조회한 URL을 포함하여 DTO를 생성합니다.
                    return new ChatMessageDto(msg, profileImageUrl);
                })
				.collect(Collectors.toList()), room.getName());
	}
	
	public void exitRoom(Long roomId, Long userId) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("User not found!"));
	    ChatRoom room = roomRepository.findById(roomId)
	            .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

	    if (!roomUserRepository.exists(userId, roomId)) {
	        System.out.println("User " + userId + " is not in room " + roomId);
	        return;
	    }

	    roomUserRepository.delete(userId, roomId);
	    eventPublisher.publishEvent(new UserExitedRoomEvent(this, userId, roomId, UserEventDto.EventType.ROOM_OUT));
	}
	
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

//	@Transactional
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

        List<ChatMessageDto> messageDtos = messages.stream()
                        .map(msg -> {
                            // 각 메시지의 작성자 프로필 이미지 URL을 조회합니다.
                            String profileImageUrl = userRepository.findProfileById(msg.getAuthor_id())
                                    .map(profileData -> (String) profileData.get("profile_image_url"))
                                    .map(url -> url != null && !url.isBlank() ? staticUrlPrefix + "/" + url : staticUrlPrefix + "/default.png")
                                    .orElse(staticUrlPrefix + "/default.png");

                            // 조회한 URL을 포함하여 DTO를 생성합니다.
                            return new ChatMessageDto(msg, profileImageUrl);
                        })
                        .collect(Collectors.toList());

        return new ChatMessageListDto(messageDtos);
	}
	
	public UserProfileDto getUserProfile(Long userId) {
		// 1. UserRepository를 사용하여 프로필 정보 조회
		Map<String, Object> profileData =
				userRepository.findProfileById(userId)
						.orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

		String imageUrl = (String) profileData.get("profile_image_url");
		
		if (imageUrl == null || imageUrl.isBlank()) {
			imageUrl = staticUrlPrefix + "/default.png"; // 기본 이미지 경로
		} else {
			imageUrl = staticUrlPrefix + "/" + imageUrl; // 저장된 이미지 경로
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

	public String updateProfileImage(Long userId, MultipartFile file) {
		// 1. 사용자 정보 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RegistrationException("NOT_FOUND", "User not found with id: " + userId));

		// 2. 기존 프로필 이미지가 있으면 삭제
		String oldFilename = user.getProfile_image_url();
		if (oldFilename != null && !oldFilename.isBlank()) {
			fileRepository.delete(oldFilename);
		}

		// 3. 새 파일 저장
		String newFilename = fileRepository.save(file);

		// 4. DB에 새 파일명 업데이트
		user.setProfile_image_url(newFilename);
		userRepository.save(user);

		// 5. 클라이언트가 접근할 수 있는 URL 경로 반환
		return staticUrlPrefix + "/" + newFilename;
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