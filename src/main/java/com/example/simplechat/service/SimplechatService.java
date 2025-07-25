package com.example.simplechat.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import java.nio.file.Files;
import java.util.Scanner;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;

import com.example.simplechat.repository.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor ;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.User;
import com.example.simplechat.model.ChatRoom;

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
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final ApplicationEventPublisher eventPublisher; // Spring의 ApplicationEventPublisher 주입
    
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository msgRepository;
    private final RoomUserRepository roomUserRepository;
    private final PasswordEncoder passwordEncoder;
	
    @PostConstruct
    public void init() {
        System.out.println("Initializing SimplechatService...");
        DB_String.configure(readTsvConfig("config.tsv"));

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
		ChatMessage newMsg = new ChatMessage( systemUser.getId(), serverChat_room.getId() );
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
			if( roomRepository.findByName(result.args).isEmpty() ) {
				ChatRoom newRoom = new ChatRoom(result.args);
				newRoom.setOwner(0L);	// system_id 0L
				if( result.options.containsKey("private") ) {
					newRoom.setRoom_type(ChatRoom.RoomType.PRIVATE);
					newRoom.setPassword_hash(result.options.get("private"));
				} else {
					newRoom.setRoom_type(ChatRoom.RoomType.PUBLIC);
				}
				roomRepository.save(newRoom);
			}
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

	public void enterRoom(Long userId, Long roomId) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("User not found!"));
	    ChatRoom room = roomRepository.findById(roomId)
	            .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

	    if (roomUserRepository.exists(userId, roomId)) {
	        System.out.println("User " + userId + " is already in room " + roomId);
	        return;
	    }

	    // 1. 사용자의 기본 닉네임을 가져옵니다.
	    String initialNickname = user.getNickname();

	    // 2. save 메서드에 초기 닉네임을 함께 전달합니다.
	    roomUserRepository.save(userId, roomId, initialNickname, "USER");

	    // 3. 이벤트 발행
	    eventPublisher.publishEvent(new UserEnteredRoomEvent(this, user, roomId));
	}
	
	public void exitRoom(Long userId, Long roomId) {
	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("User not found!"));
	    ChatRoom room = roomRepository.findById(roomId)
	            .orElseThrow(() -> new IllegalArgumentException("Room not found!"));

	    if (!roomUserRepository.exists(userId, roomId)) {
	        System.out.println("User " + userId + " is not in room " + roomId);
	        return;
	    }

	    // 1. 사용자의 기본 닉네임을 가져옵니다.
	    String initialNickname = user.getNickname();

	    // 2. save 메서드에 초기 닉네임을 함께 전달합니다.
	    roomUserRepository.delete(userId, roomId);

	    // 3. 이벤트 발행
	    eventPublisher.publishEvent(new UserExitedRoomEvent(this, userId, roomId));
	}
	
	public void changeNicknameInRoom(Long userId, Long roomId, String newNickname) {
	    // (필요하다면) 닉네임 유효성 검사 (길이, 중복 등) 로직 추가

	    // 1. DB의 chat_room_users 테이블에 있는 닉네임을 업데이트
	    roomUserRepository.updateNickname(userId, roomId, newNickname);

	    // 2. 닉네임 변경 이벤트를 발행하여 다른 사용자들에게 알림
	    //    (이 이벤트는 기존의 ChangeNicknameEvent와 다를 수 있습니다.
	    //    방 ID 정보가 포함된 새로운 이벤트 'ChangeNicknameInRoomEvent'를 만드는 것이 좋습니다.)
	    eventPublisher.publishEvent(new ChangeNicknameEvent(this, userId, roomId, newNickname));
	    System.out.println("User " + userId + "'s nickname in room " + roomId + " changed to " + newNickname);
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
		if(userRepository.existsById(userId))
			userRepository.deleteById(userId);
		else
			throw new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
		
		return userId;
	}
	
	public List<ChatRoomListDto> getRoomList(){
		return roomRepository.findAllWithCount();
	}
	
	public Long createRoom(RoomCreateDto roomcreateDto, Long userId) {
		return roomRepository.save(new ChatRoom(roomcreateDto.roomName(), 
				roomcreateDto.roomType(), userId, 
				roomcreateDto.password()!=null?passwordEncoder.encode(roomcreateDto.password()):roomcreateDto.password() ) ).getId();
	}
	
//	public void addChat(String idstr, String msgstr, String roomName) {
//		ChatRoom cr = rooms.get(roomName);
//		System.out.println(idstr);
//		cr.addChat(new ChatMessage(idstr, cr.getPop(idstr).getUsername(), msgstr));
//	}
//	
//	public List<ChatMessage> getAllChat(String roomName, Integer Id, String name){
//		ChatRoom cr = rooms.get(roomName);
//		List<ChatMessage> temp = new ArrayList<>(cr.getChats());
//
//		temp.add(new ChatMessage(""+Id, name, name, -1));	// 할당된 id 보내기
//		// 방에 있는 유저정보
//		cr.getUsers().keySet().forEach(key -> {
//			temp.add(new ChatMessage(""+key, cr.getPop(key).getUsername(), cr.getPop(key).getUsername(), -2));
//		});
//		
//		return temp;
//	}
//	
//	public boolean checkRoom(String name) { return rooms.containsKey(name); }
//    // ChatRoom 인스턴스에 ApplicationEventPublisher를 주입하는 헬퍼 메소드
//    private ChatRoom createRoomInternal(String name) {
//        ChatRoom newRoom = new ChatRoom(name);
//        newRoom.setEventPublisher(eventPublisher); // <-- 여기에서 publisher 주입!
//        rooms.put(name, newRoom);
//        System.out.println("ChatRoom created: " + name);
//        return newRoom;
//    }
//
//    // 기존 createRoom 메소드 반영 및 수정
//    public List<ChatMessage> createRoom(String name, String Id) {
//    	Integer id;
//    	String username;
//    	
//        // 이미 방이 존재하지 않는 경우에만 새로운 방을 생성하고 publisher 주입
//        if (!checkRoom(name)) {
//            createRoomInternal(name); // 새로운 방 생성 및 publisher 주입
//        }
//        ChatRoom cr = rooms.get(name);
//        
//        if( Id.equals("-1") || cr.getPop(Id) == null ) {			// id가 없으면 새로 부여하면서 생성
//        	// createUser
//        	username = "익명"+(cr.getPopsCount()+1);
//        	
//    		User ui = new User("1",username);
//    		id = ui.getId();
//    		System.out.println("new User "+id);
//    		cr.addUser(ui);
//        } else {
//        	id = Integer.parseInt(Id);
//        	username = cr.getPop(id).getUsername();
//        }
//        return getAllChat(name, id, username);
//    }
//    
//    public Map<String, ChatRoom> getAllRoom(){ return rooms; }
//    public ChatRoom getRoom(String roomName) { return rooms.get(roomName); }
//	
//	public void checkNick(String newNick, String Id, String roomName){
//		ChatRoom cr = rooms.get(roomName);
//		String oldNick = cr.getPop(Id).getUsername();
//		cr.ChangeNick(Id, newNick);
//		System.out.println("닉네임 변경 완료: "+Id+", "+oldNick+" -> "+newNick);
//	}
//	
//	
//	private ChatRoom roomNow() { return rooms.get(serv_room); }
	
	//////////////////////////////////////////////
	/// Parse config.tsv
	public static Map<String, String> readTsvConfig(String filePath) {
        Map<String, String> configMap = new HashMap<>();
        Path path = Paths.get(filePath);

        // 파일 존재 여부 및 읽기 권한 확인
        if (!Files.exists(path)) {
            System.err.println("오류: 설정 파일이 존재하지 않습니다. 경로: " + filePath);
            return configMap; // 빈 맵 반환
        }
        if (!Files.isReadable(path)) {
            System.err.println("오류: 설정 파일을 읽을 수 없습니다. 권한 문제일 수 있습니다. 경로: " + filePath);
            return configMap; // 빈 맵 반환
        }

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0; // 줄 번호 추적
            while ((line = br.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();

                // 빈 줄이거나 주석(#으로 시작)은 건너뛰기
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }

                // 첫 번째 탭 문자로 키와 값을 분리
                int firstTabIndex = trimmedLine.indexOf('\t');
                if (firstTabIndex != -1) {
                    String key = trimmedLine.substring(0, firstTabIndex).trim();
                    String value = trimmedLine.substring(firstTabIndex + 1).trim();

                    // 키가 비어있으면 경고
                    if (key.isEmpty()) {
                        System.err.println("경고 (줄 " + lineNumber + "): 키가 비어있는 항목이 발견되었습니다. 줄: '" + line + "'");
                        continue; // 이 항목은 건너뛰기
                    }

                    // Map에 저장
                    configMap.put(key, value);
                } else {
                    System.err.println("경고 (줄 " + lineNumber + "): 유효하지 않은 형식의 줄이 발견되었습니다 (탭 구분자 없음). 줄: '" + line + "'");
                }
            }
        } catch (IOException e) {
            System.err.println("설정 파일을 읽는 중 예외 발생: " + e.getMessage());
            // 실제 애플리케이션에서는 이 예외를 상위로 던지거나 더 구체적으로 처리해야 합니다.
        }
        return configMap;
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