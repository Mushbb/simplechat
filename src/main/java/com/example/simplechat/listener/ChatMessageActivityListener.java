package com.example.simplechat.listener;

import com.example.simplechat.model.User;
import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.dto.UserEventDto;
import com.example.simplechat.dto.UserEventDto.EventType;
import com.example.simplechat.event.ChatMessageAddedToRoomEvent; // 처리할 이벤트 import
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.event.ChangeNicknameEvent;
import com.example.simplechat.repository.UserRepository;

import org.springframework.context.event.EventListener; // Spring의 이벤트 리스너 어노테이션 import
import org.springframework.messaging.simp.SimpMessagingTemplate; // 웹소켓 전송 템플릿 import
import org.springframework.scheduling.annotation.Async; // 비동기 처리를 위한 어노테이션 import
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@Component // Spring Bean 으로 등록하여 이벤트 리스너로 동작하게 함
public class ChatMessageActivityListener {

    @Value("${file.profile-static-url-prefix}")
    private String profileStaticUrlPrefix;
	
    private final SimpMessagingTemplate messagingTemplate; // Spring 이 자동으로 주입해 줍니다.
    private final UserRepository userRepository;

    // Spring 의 @EventListener를 사용하여 ChatMessageAddedToRoomEvent가 발생하면 이 메소드가 호출되도록 합니다.
    // @Async 어노테이션을 사용하여 이 리스너의 동작이 비동기적으로 실행되도록 할 수 있습니다.
    @Async
    @EventListener
    public void handleChatMessageAddedToRoom(ChatMessageAddedToRoomEvent event) {
        Long roomId = event.getroomId();
        Long authorId = event.getChatMessage().getAuthor_id();

        // 사용자 프로필 이미지 URL 조회
        String profileImageUrl = userRepository.findProfileById(authorId)
                .map(profileData -> (String) profileData.get("profile_image_url"))
                .map(url -> url != null && !url.isBlank() ? profileStaticUrlPrefix + "/" + url : profileStaticUrlPrefix + "/default.png")
                .orElse(profileStaticUrlPrefix + "/default.png");

        ChatMessageDto msgDto = new ChatMessageDto(event.getChatMessage(), profileImageUrl);

        // 웹소켓으로 메시지 브로드캐스트
        try { // messagingTemplate을 사용하여 해당 토픽으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/public", msgDto);
            System.out.println("  [웹소켓 전송]: 메시지 웹소켓 전송 완료: " + msgDto.content());
        } catch (Exception e) {
            System.err.println("  [웹소켓 전송 오류]: 메시지 웹소켓 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @EventListener
    public void handleUserEnteredRoom(UserEnteredRoomEvent event) {
    	User user = event.getUser();
    	Long roomId = event.getRoomId();
    	UserEventDto userDto = new UserEventDto(EventType.ENTER, user.getId(), user.getNickname(), event.getUserType());
    	
        // 1. 웹소켓으로 메시지 브로드캐스트
        try {
            // messagingTemplate을 사용하여 해당 토픽으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            System.out.println("  [웹소켓 전송]: 유저정보 웹소켓 전송 완료: " + user.getUsername());
        } catch (Exception e) {
            System.err.println("  [웹소켓 전송 오류]: 메시지 웹소켓 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Async
    @EventListener
    public void handleUserExitedRoom(UserExitedRoomEvent event) {
    	Long userId = event.getUserId();
    	Long roomId = event.getRoomId();
    	EventType eventType = event.getEventType();
    	UserEventDto userDto = new UserEventDto(eventType, userId, null, null);
    	
        // 1. 웹소켓으로 메시지 브로드캐스트
        try {
            // messagingTemplate을 사용하여 해당 토픽으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            System.out.println("  [웹소켓 전송]: 유저정보 웹소켓 전송 완료: " + userId);
        } catch (Exception e) {
            System.err.println("  [웹소켓 전송 오류]: 메시지 웹소켓 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Async
    @EventListener
    public void handleChangeNicknameEvent(ChangeNicknameEvent event) {
    	Long userId = event.getUserId();
    	Long roomId = event.getRoomId();
    	String newNickname = event.getNewNickname();
    	
    	UserEventDto userDto = new UserEventDto(EventType.NICK_CHANGE, userId, newNickname, null);
    	
        // 1. 웹소켓으로 메시지 브로드캐스트
        try {
            // messagingTemplate을 사용하여 해당 토픽으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userDto);
            System.out.println("  [웹소켓 전송]: 유저정보 웹소켓 전송 완료: " + userId+"/"+newNickname);
        } catch (Exception e) {
            System.err.println("  [웹소켓 전송 오류]: 메시지 웹소켓 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}