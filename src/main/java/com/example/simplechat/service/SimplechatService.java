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
    private int serv_room = 0;
    
    private int flag = 1;
    private final Scanner sc = new Scanner(System.in);
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final SimpMessagingTemplate messagingTemplate;
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		// temporary
		if( rooms.size() == 0 ) {
			rooms.add(new ChatRoom("chat"));
			rooms.get(serv_room).addUser(new UserInfo(-1, "Server"));
		}
		
		if( flag == 0 )
			return;	// already scanning
		flag = 0;
		
		String text = sc.nextLine();
		if( text.startsWith("/") ) {
			if( text.startsWith("/enter") ) {
				for(int i=0;i<rooms.size();i++)
					if( rooms.get(i).getName().equals(text.split(" ")[1]) )
						serv_room = i;
			} else if( text.startsWith("/users") ) {
				System.out.println("-1: "+rooms.get(serv_room).getUsers().get(-1).getUsername()); 
				for(int i=1;i<rooms.get(serv_room).getPopsCount();i++)
					System.out.println(""+i+": "+roomNow().getUsers().get(i).getUsername()); 
			}
			return;
		}
		
		Mono.just(text).map(input -> {
			rooms.get(serv_room).addChat(-1, roomNow().getPop(-1).getUsername(), input);
            return rooms.get(serv_room).getChats().getLast();
		}).subscribe(
            chatMessage -> { // 메시지가 준비되면 웹소켓으로 전송
                messagingTemplate.convertAndSend("/topic/"+roomNow().getName()+"/public", chatMessage);
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
		Mono<ChatMessage> msgmono = Mono.just(new ChatMessage(idstr, roomNow().getPop(idstr).getUsername(), msgstr));
		
		return msgmono.map(msg -> {
			roomNow().addChat(msg);
			return msg;
		}).doOnSuccess(savedMessage -> {
			//ChatMessage temp = new ChatMessage(roomNow().getPop(Integer.parseInt(idstr)).getUsername(), msgstr, false );
			
            // 메시지가 성공적으로 저장된 후, 웹소켓 토픽으로 발행
            System.out.println("서비스: 메시지 저장 성공, 웹소켓 발행 시작: "+savedMessage.getId() +", "+savedMessage.getName()+", "+ savedMessage.getChat());
            messagingTemplate.convertAndSend("/topic/"+roomNow().getName()+"/public", savedMessage); // 핵심 라인!
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
		ChatRoom cr = rooms.get(serv_room);
		List<ChatMessage> temp = new ArrayList<>(cr.getChats());
		String name = "익명"+cr.getPopsCount();
		int id = cr.getPopsCount();
		cr.addUser(new UserInfo(name));
		
		List<ChatMessage> temp1 = temp.stream()
			.map(msg -> new ChatMessage(msg.getId(), msg.getName(), msg.getChat(), false))
			.collect(Collectors.toList());

		temp1.add(new ChatMessage(""+id, name, name, -1));
		return Mono.just(temp1);
	}
	
	public void checkNick(String newNick, String Id){
		ChatRoom cr = rooms.get(serv_room);
		cr.ChangeNick(Id, newNick);
		System.out.println("닉네임 변경 완료:"+Id+" -> "+newNick);
	}
	
	private int strtoint(String input) { return Integer.parseInt(input); }
	private ChatRoom roomNow() { return rooms.get(serv_room); }

	@PreDestroy
	public void closeScanner() { sc.close(); }
}