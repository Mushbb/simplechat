package com.example.simplechat.controller;

import com.example.simplechat.service.SimplechatService;
import com.example.simplechat.model.ChatMessage;		// DTO를 만들어서 제거해주기
import com.example.simplechat.model.ChatRoom;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
public class simplechatController {
	// 1. 서비스 객체를 참조할 필드 선언 (불변성을 위해 final로 선언)
	private final SimplechatService serv;
	
	// 2. 생성자를 통한 의존성 주입 (가장 권장되는 방법)
    // 스프링이 UserService 타입의 빈(객체)을 찾아서 이 생성자의 파라미터로 주입해 줍니다.
	public simplechatController(SimplechatService serv) {
		this.serv = serv;
	}
	/*
	@GetMapping("/")	// 한번도 호출되고있지 않은거?
	public Flux<ChatMessage> catchGetRequests(HttpServletRequest request) {
        //String requestURI = request.getRequestURI();
        System.out.println("호출되고 있다구!");
        return serv.getChat();
    }*/
	
	@PostMapping("/lobby")
	public Mono<List<String[]>> getRoomList(){
		// Map의 각 엔트리(방 이름, ChatRoom 객체)를 순회하며 필요한 정보만 추출
        return Mono.just(serv.getAllRoom().entrySet().stream()
            .map(entry -> {
                String roomName = entry.getKey();
                ChatRoom room = entry.getValue();
                // 여기에 필요한 다른 정보(예: 사용자 수)를 추가할 수 있습니다.
                // 임시로 [방 이름, 방 이름] 형태로 반환 (ID와 Name 개념으로)
                // 만약 room.getId()와 같은 필드가 있다면 활용 가능
                return new String[]{roomName, roomName, ""+room.getPopsCount()}; // [ID, Name, Pop] 형태로 가정
            })
            .collect(Collectors.toList()));
	}
	
	@PostMapping("/{roomName}")
	public Mono<List<ChatMessage>> catchAllGetRequests(@PathVariable("roomName") String path) {
		System.out.println("All"+path);
        return serv.createRoom(path);
    }
	
	@PostMapping("/{roomName}/chat")
	public void recvMessage(@RequestParam("message") String request, @RequestParam("id") String newId, @PathVariable("roomName") String path) {
		System.out.println(path);
		serv.addChat(newId, request, path);
	}
	
	@PostMapping("/{roomName}/nick")
	public void recvNick(@RequestParam("nick") String newNick, @RequestParam("id") String Id, @PathVariable("roomName") String path) {
		serv.checkNick(newNick, Id, path);
	}
}