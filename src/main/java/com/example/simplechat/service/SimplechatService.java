package com.example.simplechat.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.Scanner;
import jakarta.annotation.PreDestroy;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.UserInfo;
import com.example.simplechat.model.ChatRoom;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동 생성
@EnableScheduling
public class SimplechatService {
    private final Map<String, ChatRoom> rooms = new HashMap<>();
    private String serv_room = "chat";
    
    private int flag = 1;
    private final Scanner sc = new Scanner(System.in);
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
//    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher; // Spring의 ApplicationEventPublisher 주입
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		// temporary
//		if( rooms.size() == 0 ) {
//			rooms.put(serv_room, new ChatRoom(serv_room));
//			rooms.get(serv_room).addUser(new UserInfo(-1, "Server"));
////		}
//		
//		if( flag == 0 )
//			return;	// already scanning
//		if( !checkRoom(serv_room) ) {
//			System.out.println("there is no room: "+serv_room);
//			createRoomInternal(serv_room);
//			return;
//		}
		String text = sc.nextLine();
		
		if( text.startsWith("/") ) {
			if( text.startsWith("/enter") ) {
				serv_room = text.split(" ")[1];
				System.out.println(serv_room+"으로 이동");
				if( !checkRoom(serv_room) ) {
					System.out.println("there is no room: "+serv_room);
					createRoomInternal(serv_room);
					return;
				}
			} else if( text.startsWith("/users") ) {
				System.out.println("-1: "+roomNow().getAdmin().getUsername()); 
				for(Integer key : roomNow().getUsers().keySet() ) {
					System.out.println(""+key+": "+roomNow().getUsers().get(key).getUsername()); 
				}
					
			} else if( text.startsWith("/clear") ) {
				if( !checkRoom(serv_room) )
					System.out.println(serv_room+"는 없는 방입니다.");
				else {
					System.out.println("send /clear");
					roomNow().addChat(new ChatMessage("-1", "Server", "/clear", -1));	// -1: message from server
				}
			}
			
			return;
		}
		
		roomNow().addChat(roomNow().getAdmin().getId(), roomNow().getAdmin().getUsername(), text);
		
//      return roomNow().getChats().getLast();
		
//		flag = 0;
//		
//		flag = 1;
//		Mono.just(text).map(input -> {
//			roomNow().addChat(roomNow().getAdmin().getId(), roomNow().getAdmin().getUsername(), input);
//            return roomNow().getChats().getLast();
//		}).subscribe(
//            chatMessage -> { // 메시지가 준비되면 웹소켓으로 전송
//                messagingTemplate.convertAndSend("/topic/"+roomNow().getName()+"/public", chatMessage);
//                System.out.println("서버 메시지 전송 완료: " + chatMessage.getChat());
//            },
//            error -> { // 오류 처리
//                System.err.println("메시지 전송 중 오류 발생: " + error.getMessage());
//                error.printStackTrace();
//            },
//            () -> { // 스트림 완료 (여기서는 한 번의 입력 후 완료)
//                //flag = 1; // 입력 대기 플래그 다시 설정
//            }
//        );
	}

	public void addChat(String idstr, String msgstr, String roomName) {
		ChatRoom cr = rooms.get(roomName);
		System.out.println(idstr);
		cr.addChat(new ChatMessage(idstr, cr.getPop(idstr).getUsername(), msgstr));
//		Mono<ChatMessage> msgmono = Mono.just(new ChatMessage(idstr, cr.getPop(idstr).getUsername(), msgstr));
//		
//		return msgmono.map(msg -> {
//			cr.addChat(msg);
//			return msg;
//		}).doOnSuccess(savedMessage -> {
//			//ChatMessage temp = new ChatMessage(roomNow().getPop(Integer.parseInt(idstr)).getUsername(), msgstr, false );
//			
//            // 메시지가 성공적으로 저장된 후, 웹소켓 토픽으로 발행
//            System.out.println("서비스: 메시지 저장 성공, 웹소켓 발행 시작: "+savedMessage.getId() +", "+savedMessage.getName()+", "+ savedMessage.getChat());
//            messagingTemplate.convertAndSend("/topic/"+cr.getName()+"/public", savedMessage); // 핵심 라인!
//        }).then();
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
	
	public List<ChatMessage> getAllChat(String roomName, Integer Id, String name){
		ChatRoom cr = rooms.get(roomName);
		List<ChatMessage> temp = new ArrayList<>(cr.getChats());
		
//		List<ChatMessage> temp1 = temp.stream()
//			.map(msg -> new ChatMessage(msg.getId(), msg.getName(), msg.getChat(), false))
//			.collect(Collectors.toList());

		temp.add(new ChatMessage(""+Id, name, name, -1));	// 할당된 id 보내기
		// 방에 있는 유저정보
		cr.getUsers().keySet().forEach(key -> {
			temp.add(new ChatMessage(""+key, cr.getPop(key).getUsername(), cr.getPop(key).getUsername(), -2));
		});
		
		return temp;
	}
	
	public boolean checkRoom(String name) { return rooms.containsKey(name); }
    // ChatRoom 인스턴스에 ApplicationEventPublisher를 주입하는 헬퍼 메소드
    private ChatRoom createRoomInternal(String name) {
        ChatRoom newRoom = new ChatRoom(name);
        newRoom.setEventPublisher(eventPublisher); // <-- 여기에서 publisher 주입!
        rooms.put(name, newRoom);
        System.out.println("ChatRoom created: " + name);
        return newRoom;
    }

    // 기존 createRoom 메소드 반영 및 수정
    public List<ChatMessage> createRoom(String name, String Id) {
    	int id;
    	String username;
    	
        // 이미 방이 존재하지 않는 경우에만 새로운 방을 생성하고 publisher 주입
        if (!checkRoom(name)) {
            createRoomInternal(name); // 새로운 방 생성 및 publisher 주입
        }
        ChatRoom cr = rooms.get(name);
        
        if( Id.equals("-1") || cr.getPop(Id) == null ) {			// id가 없으면 새로 부여하면서 생성
        	// createUser
        	username = "익명"+(cr.getPopsCount()+1);
        	
    		UserInfo ui = new UserInfo(username);
    		id = ui.getId();
    		System.out.println("new User "+id);
    		cr.addUser(ui);
        } else {
        	id = Integer.parseInt(Id);
        	username = cr.getPop(id).getUsername();
        }
        return getAllChat(name, id, username);
    }
    
    public Map<String, ChatRoom> getAllRoom(){ return rooms; }
    public ChatRoom getRoom(String roomName) { return rooms.get(roomName); }
	
	public void checkNick(String newNick, String Id, String roomName){
		ChatRoom cr = rooms.get(roomName);
		String oldNick = cr.getPop(Id).getUsername();
		cr.ChangeNick(Id, newNick);
		System.out.println("닉네임 변경 완료: "+Id+", "+oldNick+" -> "+newNick);
	}
	
	private int strtoint(String input) { return Integer.parseInt(input); }
	private ChatRoom roomNow() { return rooms.get(serv_room); }

	@PreDestroy
	public void closeScanner() { sc.close(); }
}