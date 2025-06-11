package com.example.simplechat.service;

import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;
import java.util.Scanner;
import jakarta.annotation.PreDestroy;
import java.util.stream.Collectors;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.UserInfo;
import com.example.simplechat.model.ChatRoom;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동 생성
@EnableScheduling
public class SimplechatService {
    private final List<ChatRoom> rooms = new ArrayList<>();
    
    private int flag = 1;
    private final Scanner sc = new Scanner(System.in);
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final SimpMessagingTemplate messagingTemplate;
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		// temporary
		if( rooms.size() == 0 ) {
			rooms.add(new ChatRoom("chat"));
			rooms.get(0).addUser(new UserInfo(-1, "server"));
		}
		
		if( flag == 0 ) return;	// already scanning
		flag = 0;
		
		String text = sc.nextLine();
		
		Mono.just(text).map(input -> {
			rooms.get(0).addChat(-1, input);
            return rooms.get(0).getChats().getLast();
		}).subscribe(
            chatMessage -> { // 메시지가 준비되면 웹소켓으로 전송
                messagingTemplate.convertAndSend("/topic/"+rooms.get(0).getName()+"/public", chatMessage);
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

	public Mono<Void> addChat(String idstr, String msgstr) {
		Mono<ChatMessage> msgmono = Mono.just(new ChatMessage(idstr, msgstr, false));
		
		return msgmono.map(msg -> {
			rooms.get(0).addChat(msg);
			return msg;
		}).doOnSuccess(savedMessage -> {
			ChatMessage temp = new ChatMessage(rooms.get(0).getPop(Integer.parseInt(idstr)).getUsername(), msgstr, false );
			
            // 메시지가 성공적으로 저장된 후, 웹소켓 토픽으로 발행
            System.out.println("서비스: 메시지 저장 성공, 웹소켓 발행 시작: " + temp);
            messagingTemplate.convertAndSend("/topic/"+rooms.get(0).getName()+"/public", temp); // 핵심 라인!
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
	public Mono<List<ChatMessage>> getAllChat(){
		ChatRoom cr = rooms.get(0);
		List<ChatMessage> temp = new ArrayList<>(cr.getChats());
		String name = "익명"+cr.getPopsCount();
		int id = cr.getPopsCount();
		cr.addUser(new UserInfo(name));
		
		List<ChatMessage> temp1 = temp.stream()
			.map(msg -> new ChatMessage(cr.getPop( msg.getId() ).getUsername(), msg.getChat(), false))
			.collect(Collectors.toList());

		temp1.add(new ChatMessage(id, name, -1));
		return Mono.just(temp1);
	}
	
	public void checkNick(String newNick, String Id){
		ChatRoom cr = rooms.get(0);
		cr.ChangeNick(Id, newNick);
	}

	@PreDestroy
	public void closeScanner() { sc.close(); }
}