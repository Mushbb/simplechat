package com.example.simplechat.service;

import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Scanner;
import jakarta.annotation.PreDestroy;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.UserInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동 생성
@EnableScheduling
public class SimplechatService {
	// 필드 선언 및 선언 시 초기화 (final이 아닌 필드도 가능)
    private final List<ChatMessage> chats = new ArrayList<>(); // final로 선언하고 바로 초기화
    private final List<UserInfo> users = new ArrayList<>();
    private String text = "initialized";
    private int flag = 1;
    private int pops = 0;		// 방에 몇명인지 (나중에 chatroom class)
    private final Scanner sc = new Scanner(System.in); // final로 선언하고 바로 초기화
    private int MAX_ALLCHAT = 200;
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final SimpMessagingTemplate messagingTemplate;
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		if( flag == 0 ) return;	// already scanning
		flag = 0;
		
		text = sc.nextLine();
		
		Mono.just(text).map(input -> {
			ChatMessage msg = new ChatMessage();
            msg.setId("server");
            msg.setChat(input);
            synchronized (chats) { // 리스트 동기화
                msg.setMessageNum(chats.size());
                chats.add(msg);
            }
            return msg;
		}).subscribe(
            chatMessage -> { // 메시지가 준비되면 웹소켓으로 전송
                messagingTemplate.convertAndSend("/topic/public", chatMessage);
                System.out.println("서버 메시지 전송 완료: " + chatMessage.getChat());
            },
            error -> { // 오류 처리
                System.err.println("메시지 전송 중 오류 발생: " + error.getMessage());
                error.printStackTrace();
            },
            () -> { // 스트림 완료 (여기서는 한 번의 입력 후 완료)
                flag = 1; // 입력 대기 플래그 다시 설정
            }
        );
	}
	
	public String getText() { return this.text;	}
	public void setText(String str) { this.text = str; }
	public Mono<Void> addChat(Mono<ChatMessage> msgmono) {
		return msgmono.map(msg -> {
			ChatMessage temp = new ChatMessage();
			temp.setId(msg.getId());
			temp.setChat(msg.getChat());
			temp.setMessageNum(msg.getMessageNum());
			chats.add(temp);
			return msg;
		}).doOnSuccess(savedMessage -> {
            // 메시지가 성공적으로 저장된 후, 웹소켓 토픽으로 발행
            System.out.println("서비스: 메시지 저장 성공, 웹소켓 발행 시작: " + savedMessage);
            messagingTemplate.convertAndSend("/topic/public", savedMessage); // 핵심 라인!
        }).then();
	}
	/*
	public Flux<ChatMessage> getChat(){ 
		List<ChatMessage> sending = null;
		if( sended == -1 ) {
			sending = chats;
		} else {	// from sended to chats.size()
			sending = chats.subList(sended, chats.size());
		}
		sended = chats.size();
		return Flux.fromIterable(sending);
	}*/
	public Flux<ChatMessage> getAllChat(){
		List<ChatMessage> temp = new ArrayList<>(chats);
		users.add(new UserInfo("익명"+(users.size()+1)));
		temp.add(new ChatMessage("Anonymous",users.getLast().getUsername(),-1));
		int size = chats.size()+1;	// with invisible variable
		return Flux.fromIterable(temp.subList(size>=MAX_ALLCHAT?size-MAX_ALLCHAT:0, size));
	}
	public int getChatSize() { return chats.size(); }
	
	public Mono<Void> checkNick(Mono<String> newId, String oldId){
		return newId.map(nick -> {
			for(int i=0;i<users.size();i++)
				if(users.get(i).getUsername().equals(nick))
					return oldId+"/1";
			// if not change nick on server
			users.forEach(user -> {
				if( user.getUsername() == oldId ) {
					user.setUsername(nick);
				}
			});
			
			return oldId+"/0";
		}).doOnSuccess(savedMessage -> {
            // 메시지가 성공적으로 저장된 후, 웹소켓 토픽으로 발행
            System.out.println("서비스: 메시지 저장 성공, 웹소켓 발행 시작: " + "/topic/nick/" +", "+ savedMessage);
            messagingTemplate.convertAndSend("/topic/nick/", savedMessage); // 핵심 라인!
        }).then();
	}
	
	@PreDestroy
	public void closeScanner() { sc.close(); }
}