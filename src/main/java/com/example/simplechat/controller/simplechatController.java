package com.example.simplechat.controller;

import com.example.simplechat.service.SimplechatService;
import com.example.simplechat.model.User;
import com.example.simplechat.exception.*;
import com.example.simplechat.dto.*;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor 
@RestController
public class simplechatController {
	// 1. 서비스 객체를 참조할 필드 선언 (불변성을 위해 final로 선언)
	private final SimplechatService serv;
	
	@PostMapping("/auth/register")
	public LoginResponseDto registerRequest(@RequestBody UserRegistrationRequestDto requestDto, HttpSession session) {
		User registered = serv.register(requestDto);
		
		 // 2. Spring Security가 인식할 인증 토큰(공식 출입증) 생성
	    Authentication authentication = new UsernamePasswordAuthenticationToken(
	        registered.getUsername(),
	        null,
	        new ArrayList<>()
	    );
	    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
	    securityContext.setAuthentication(authentication);
	    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
	    
		session.setAttribute("userId", registered.getId());
		session.setMaxInactiveInterval(180 * 60); // 180분 동안 비활성 시 세션 만료
		
		return new LoginResponseDto(
			registered.getId(),
			registered.getUsername(),
			registered.getNickname()
		);
	}
	
	@PostMapping("/auth/login")
	public LoginResponseDto loginRequest(@RequestBody LoginRequestDto requestDto, HttpSession session) {
		User loggedIn = serv.login(requestDto);
		
		// 2. 인증 토큰(공식 출입증) 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            loggedIn.getUsername(), // principal: 주로 사용자 ID나 객체를 넣습니다.
            null,                  // credentials: 비밀번호는 이미 검증했으므로 null 처리합니다.
            new ArrayList<>()      // authorities: 사용자의 권한 목록 (지금은 빈 리스트)
        );
		
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        
		session.setAttribute("userId", loggedIn.getId());
		session.setMaxInactiveInterval(180 * 60); // 180분 동안 비활성 시 세션 만료
		
		return new LoginResponseDto(
			loggedIn.getId(),
			loggedIn.getUsername(),
			loggedIn.getNickname()
		);
	}
	
	@GetMapping("/auth/session")
	public LoginResponseDto sessionCheck(HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		System.out.println("UserId: "+userId);
		if( userId == null ) {
			throw new RegistrationException("UNAUTHORIZED", "Not Logged In");
		}
		
		User loggedIn = serv.getUserById(userId);
		return new LoginResponseDto(
			loggedIn.getId(),
			loggedIn.getUsername(),
			loggedIn.getNickname()
		);
	}
	
	@PostMapping("/auth/logout")	// /logout은 예약엔드포인트였어..
	public Integer logoutRequest(HttpSession session) {
		System.out.println("Session Closed: "+session.getAttribute("userId"));
		session.invalidate(); // 세션 무효화
	    return 1;
	}
	
	@DeleteMapping("/auth/delete")
	public Integer deleteRequest(HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		serv.delete_account(userId);
		System.out.println("Session Closed: "+userId);
		session.invalidate(); // 세션 무효화
		return 1;
	}
	
	@GetMapping("/api/my-rooms")
	public ResponseEntity<List<ChatRoomListDto>> getMyRooms(HttpSession session) {
	    Long userId = (Long) session.getAttribute("userId");
	    if (userId == null) {
	        // 로그인하지 않은 사용자는 빈 목록 또는 에러를 반환
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    }
	    // ✅ 서비스에 userId로 참여 중인 방 목록을 가져오는 로직이 필요합니다.
	    List<ChatRoomListDto> myRooms = serv.findRoomsByUserId(userId);
	    return ResponseEntity.ok(myRooms);
	}
	
	@GetMapping("/user/{userId}/profile")
	public UserProfileDto getUserProfile(@PathVariable("userId") Long userId) {
		return serv.getUserProfile(userId);
	}
	
	@PutMapping("/user/profile")
	public UserProfileDto changeUserProfile(@RequestBody ProfileUpdateRequestDto profileDto, HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		
		return serv.changeUserProfile(profileDto, userId);
	}

	@PostMapping("/user/profile/image")
	public ResponseEntity<Map<String, String>> uploadProfileImage(
		@RequestParam("profileImage") MultipartFile file,
		HttpSession session
	) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			throw new RegistrationException("UNAUTHORIZED", "Please login first!");
		}
		String newImageUrl = serv.updateProfileImage(userId, file);

		// 클라이언트가 즉시 이미지를 업데이트할 수 있도록 새 이미지 URL을 반환
		return ResponseEntity.ok(Map.of("profileImageUrl", newImageUrl));
	}

	@PostMapping("/room/{roomId}/file")
	public ResponseEntity<Void> uploadChatFile(
			@PathVariable("roomId") Long roomId,
			@RequestParam("file") MultipartFile file,
			HttpSession session) {

		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			throw new RegistrationException("UNAUTHORIZED", "Please login first!");
		}

		serv.uploadChatFile(roomId, userId, file);

		return ResponseEntity.ok().build();
	}
	
	
	@GetMapping("/room/list")
	public List<ChatRoomListDto> getRoomList(HttpSession session){
		Long userId = (Long)session.getAttribute("userId");
//		if( userId == null ) {
//			throw new RegistrationException("UNAUTHORIZED","Please login first!");
//		}
        return serv.getRoomList(userId);
	}
	
	@PostMapping("/room/create")
	public Long createRoom(@RequestBody RoomCreateDto roomcreateDto, HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		if( userId == null ) {
			throw new RegistrationException("UNAUTHORIZED","Please login first!");
		}
		return serv.createRoom(roomcreateDto, userId);
	}
	
	@PostMapping("/room/{roomId}/users")
	public Long enterRoom(@PathVariable("roomId") Long roomId, @RequestBody RoomEnterDto enterDto, HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		if( userId == null ) {
			throw new RegistrationException("UNAUTHORIZED","Please login first!");
		}
		
		return serv.enterRoom(roomId, userId, enterDto.password());
	}
	

	
	@DeleteMapping("/room/{roomId}/users")
	public void exitRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
	    Long userId = (Long) session.getAttribute("userId");
	    if (userId == null) {
	        // 이 경우는 거의 없겠지만, 안전을 위해 추가
	        throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
	    }
	    serv.exitRoom(roomId, userId);
	}

	@DeleteMapping("/api/rooms/{roomId}/users/{userId}")
	public void kickUserFromRoom(@PathVariable("roomId") Long roomId, @PathVariable("userId") Long userIdToKick, HttpSession session) {
		Long kickerId = (Long) session.getAttribute("userId");
		if (kickerId == null) {
			throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
		}
		serv.kickUser(roomId, kickerId, userIdToKick);
	}

	@DeleteMapping("/room/{roomId}")
	public void deleteRoom(@PathVariable("roomId") Long roomId, HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
		}
		serv.deleteRoom(roomId, userId);
	}
	
	@GetMapping("/room/{roomId}/init")
	public RoomInitDataDto initRoom(@PathVariable("roomId") Long roomId, @RequestParam(name="lines", defaultValue="20") int lines,HttpSession session) {
		Long userId = (Long)session.getAttribute("userId");
		
		return serv.initRoom(roomId, userId, lines);
	}
	
	@MessageMapping("/chat.sendMessage")
	public void recvMessage(ChatMessageRequestDto msgDto) {
		serv.addChat_publish(msgDto);
	}
	
	@MessageMapping("/chat.changeNick")
	public void changeNick(NickChangeDto nickChangeDto) {
		serv.changeNicknameInRoom(nickChangeDto);
	}
	
	// =================================================================================================
	// Friendship API
	// =================================================================================================

	@PostMapping("/api/friends/requests")
	public ResponseEntity<Void> sendFriendRequest(@RequestBody FriendRequestDto requestDto, HttpSession session) {
		Long senderId = (Long) session.getAttribute("userId");
		if (senderId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		serv.sendFriendRequest(senderId, requestDto.receiverId());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/friends")
	public ResponseEntity<List<FriendResponseDto>> getFriends(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(serv.getFriends(userId));
	}

	@DeleteMapping("/api/friends/{friendId}")
	public ResponseEntity<Void> removeFriend(@PathVariable("friendId") Long friendId, HttpSession session) {
		Long removerId = (Long) session.getAttribute("userId");
		if (removerId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		serv.removeFriend(removerId, friendId);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/api/friends/status/{otherUserId}")
	public ResponseEntity<Map<String, String>> getFriendshipStatus(@PathVariable("otherUserId") Long otherUserId, HttpSession session) {
		Long currentUserId = (Long) session.getAttribute("userId");
		if (currentUserId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(serv.getFriendshipStatus(currentUserId, otherUserId));
	}
	
    @PostMapping("/room/{roomId}/invite")
    public ResponseEntity<Void> inviteUserToRoom(
            @PathVariable("roomId") Long roomId,
            @RequestBody InviteRequestDto inviteDto,
            HttpSession session) {
        
        Long inviterId = (Long) session.getAttribute("userId");
        if (inviterId == null) {
            throw new RegistrationException("UNAUTHORIZED", "로그인이 필요합니다.");
        }

        serv.inviteUserToRoom(roomId, inviterId, inviteDto.userId());

        return ResponseEntity.ok().build();
    }

    // ✨ 신규: 통합 알림 목록 조회 API
    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(serv.getPendingNotifications(userId));
    }

    // ✨ 신규: 통합 알림 수락 API
    @PutMapping("/api/notifications/{notificationId}/accept")
    public ResponseEntity<Void> acceptNotification(@PathVariable("notificationId") Long notificationId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        serv.acceptNotification(notificationId, userId);
        return ResponseEntity.ok().build();
    }
    
    // ✨ 신규: 통합 알림 거절 API
    @DeleteMapping("/api/notifications/{notificationId}/reject")
    public ResponseEntity<Void> rejectNotification(@PathVariable("notificationId") Long notificationId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        serv.rejectNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ✨ 신규: 관리자 명령어 실행 API
    @PostMapping("/api/admin/command")
    public ResponseEntity<Map<String, String>> executeAdminCommand(@RequestBody Map<String, String> payload, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        // 관리자(userId=0)가 아니면 접근 거부
        if (userId == null || userId != 0) {
            throw new RegistrationException("FORBIDDEN", "관리자만 사용할 수 있는 기능입니다.");
        }

        String command = payload.get("command");
        if (command == null || command.isBlank()) {
            throw new RegistrationException("BAD_REQUEST", "실행할 명령어를 입력해주세요.");
        }

        // 서비스에 명령어 실행을 위임하고 결과를 받음
        String result = serv.executeAdminCommand(command);

        // 실행 결과를 클라이언트에 반환
        return ResponseEntity.ok(Map.of("message", result));
    }

    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable("messageId") Long messageId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        serv.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> editMessage(
            @PathVariable("messageId") Long messageId,
            @RequestBody Map<String, String> payload,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new RegistrationException("UNAUTHORIZED", "세션 정보를 찾을 수 없습니다.");
        }
        String newContent = payload.get("content");
        serv.editMessage(messageId, userId, newContent);
        return ResponseEntity.noContent().build();
    }
	
	@MessageMapping("/chat.getMessageList")
	@SendToUser("/topic/queue/reply")
	public ChatMessageListDto getMessageList(ChatMessageListRequestDto msgListDto) {
		return serv.getMessageList(msgListDto);
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////
///	SQL TEST End-points
//	
//	@PostMapping("/test/shop")
//	public List<String> shop(@RequestParam("sqlQuery") String sqlQuery) {
//		// JDBC 연결해서 쿼리문 결과 List<>리턴 -> JSON 자동변환
//		// 헤더도 포함해서 넣어줘야해
//		return JDBC_SQL.excuteQuery(sqlQuery);
//	}
//	
//	@PostMapping("/test/login")
//	public Map<String, Object> login(@RequestParam(value="user_id",required=false) String userid, 
//			@RequestParam(value="user_pw",required=false) String userpw, HttpSession session) {
//		Map<String, Object> responseBody = new HashMap<>();
//		// 1. 현재 세션에 이미 로그인된 사용자가 있는지 확인
//        String loggedInUser = (String) session.getAttribute("loggedInUser");
//        
//        if (loggedInUser != null) {
//            // 이미 이 사용자가 로그인되어 있다면
//            System.out.println("DEBUG: User '" + loggedInUser + "' is already logged in. Returning success.");
//            responseBody.put("flag", 3);
//        } else {
//        	Integer logged = JDBC_SQL.login(userid, userpw);
//    		if( logged == 1 ) {
//    			session.setAttribute("loggedInUser", userid); // 세션에 로그인 사용자 정보 저장
//    			System.out.print(session.getAttribute("loggedInUser")+"\n");
//    			loggedInUser = userid;
//                session.setMaxInactiveInterval(30 * 60); // 30분 동안 비활성 시 세션 만료
//                responseBody.put("flag", 1);
//    		} else {
//    			responseBody.put("flag", 2);
//    		}
//        }
//		responseBody.put("userId", loggedInUser);
//		
//		System.out.print("end: "+session.getAttribute("loggedInUser")+"\n");
//		return responseBody;
//	}
//	
//	// 리다이렉트 처리
//	@PostMapping("/test/redirect")
//	public Map<String, Object> redirect(String URL) {
//		Map<String, Object> responseBody = new HashMap<>();
//		
//		responseBody.put("URL", URL);
//		
//		return responseBody;
//	}
//	
//	// 로그아웃 처리
//    @PostMapping("/test/logout")
//    public Integer logout(HttpSession session) {
//    	System.out.println("Session Closed: "+session.getAttribute("loggedInUser"));
//        session.invalidate(); // 세션 무효화
//        return 1;
//    }
}