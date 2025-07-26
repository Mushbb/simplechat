package com.example.simplechat.listener;

import com.example.simplechat.model.User;
import com.example.simplechat.dto.ChatMessageDto;
import com.example.simplechat.dto.UserEventDto;
import com.example.simplechat.dto.UserEventDto.EventType;
import com.example.simplechat.event.ChatMessageAddedToRoomEvent; // 처리할 이벤트 import
import com.example.simplechat.event.UserEnteredRoomEvent;
import com.example.simplechat.event.UserExitedRoomEvent;
import com.example.simplechat.event.ChangeNicknameEvent;

import org.springframework.context.event.EventListener; // Spring의 이벤트 리스너 어노테이션 import
import org.springframework.messaging.simp.SimpMessagingTemplate; // 웹소켓 전송 템플릿 import
import org.springframework.scheduling.annotation.Async; // 비동기 처리를 위한 어노테이션 import
import org.springframework.stereotype.Component;

@Component // Spring Bean 으로 등록하여 이벤트 리스너로 동작하게 함
public class ChatMessageActivityListener {

    private final SimpMessagingTemplate messagingTemplate; // Spring 이 자동으로 주입해 줍니다.

    // 생성자를 통해 SimpMessagingTemplate을 주입받습니다.
    public ChatMessageActivityListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Spring 의 @EventListener를 사용하여 ChatMessageAddedToRoomEvent가 발생하면 이 메소드가 호출되도록 합니다.
    // @Async 어노테이션을 사용하여 이 리스너의 동작이 비동기적으로 실행되도록 할 수 있습니다.
    @Async
    @EventListener
    public void handleChatMessageAddedToRoom(ChatMessageAddedToRoomEvent event) {
        Long roomId = event.getroomId();
        ChatMessageDto msgDto = new ChatMessageDto(event.getChatMessage());

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
    	UserEventDto userDto = new UserEventDto(EventType.EXIT, userId, null, null);
    	
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
    	
        // 1. 웹소켓으로 메시지 브로드캐스트
        try {
            // messagingTemplate을 사용하여 해당 토픽으로 메시지 전송
            messagingTemplate.convertAndSend("/topic/" + roomId + "/users", userId+"/"+newNickname);
            System.out.println("  [웹소켓 전송]: 유저정보 웹소켓 전송 완료: " + userId+"/"+newNickname);
        } catch (Exception e) {
            System.err.println("  [웹소켓 전송 오류]: 메시지 웹소켓 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}