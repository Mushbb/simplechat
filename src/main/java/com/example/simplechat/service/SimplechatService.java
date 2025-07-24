package com.example.simplechat.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Files;
import java.util.Scanner;
import jakarta.annotation.PreDestroy;

import com.example.simplechat.repository.DB_String;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor ;

import com.example.simplechat.model.ChatMessage;
import com.example.simplechat.model.User;
import com.example.simplechat.model.ChatRoom;

@Service
@RequiredArgsConstructor 
@EnableScheduling
public class SimplechatService {
    private final Map<String, ChatRoom> rooms = new HashMap<>();	// Replace to RoomRepository class
    private final Map<Integer, User> users = new HashMap<>();		// Replace to UserRepository class
    																// Add MessageRepository class
    
    
    private String serv_room = "chat";
    private final Scanner sc = new Scanner(System.in);
	
	// SimpMessagingTemplate 주입 (웹소켓 메시지를 발행하는 데 사용)
    private final ApplicationEventPublisher eventPublisher; // Spring의 ApplicationEventPublisher 주입
    private final Map<String, String> config = readTsvConfig("config.tsv");
    private final DB_String DB_Instance = DB_String.configure(config);
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
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
	}

	public void addChat(String idstr, String msgstr, String roomName) {
		ChatRoom cr = rooms.get(roomName);
		System.out.println(idstr);
		cr.addChat(new ChatMessage(idstr, cr.getPop(idstr).getUsername(), msgstr));
	}
	
	public List<ChatMessage> getAllChat(String roomName, Integer Id, String name){
		ChatRoom cr = rooms.get(roomName);
		List<ChatMessage> temp = new ArrayList<>(cr.getChats());

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
    	Integer id;
    	String username;
    	
        // 이미 방이 존재하지 않는 경우에만 새로운 방을 생성하고 publisher 주입
        if (!checkRoom(name)) {
            createRoomInternal(name); // 새로운 방 생성 및 publisher 주입
        }
        ChatRoom cr = rooms.get(name);
        
        if( Id.equals("-1") || cr.getPop(Id) == null ) {			// id가 없으면 새로 부여하면서 생성
        	// createUser
        	username = "익명"+(cr.getPopsCount()+1);
        	
    		User ui = new User("1",username);
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
}