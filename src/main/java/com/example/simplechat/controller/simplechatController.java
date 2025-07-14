package com.example.simplechat.controller;

import com.example.simplechat.service.SimplechatService;
import com.example.simplechat.model.ChatMessage;		// DTO를 만들어서 제거해주기
import com.example.simplechat.model.ChatRoom;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.stream.Collectors;

import com.example.jdbctest.jdbctest;

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
	public List<String[]> getRoomList(){
		// System.out.println("lobbylist");
		// Map의 각 엔트리(방 이름, ChatRoom 객체)를 순회하며 필요한 정보만 추출
        return serv.getAllRoom().entrySet().stream()
            .map(entry -> {
                String roomName = entry.getKey();
                ChatRoom room = entry.getValue();
                // 여기에 필요한 다른 정보(예: 사용자 수)를 추가할 수 있습니다.
                // 임시로 [방 이름, 방 이름] 형태로 반환 (ID와 Name 개념으로)
                // 만약 room.getId()와 같은 필드가 있다면 활용 가능
                return new String[]{roomName, roomName, ""+room.getPopsCount()}; // [ID, Name, Pop] 형태로 가정
            })
            .collect(Collectors.toList());
	}
	
	@PostMapping("/{roomName}")
	public List<ChatMessage> catchAllGetRequests(@PathVariable("roomName") String path, @RequestParam("id") String Id) {
//		System.out.println("new User in "+path+", "+Id);
        return serv.createRoom(path, Id);
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
	
	
	
	
	
	
	@PostMapping("/shop")
	public List<String> shop(@RequestParam("sqlQuery") String sqlQuery) {
		// JDBC 연결해서 쿼리문 결과 List<>리턴 -> JSON 자동변환
		// 헤더도 포함해서 넣어줘야해
		return jdbctest.excuteQuery(sqlQuery);
	}
	
	@PostMapping("/login")
	public Integer login(@RequestParam("user_id") String userid, @RequestParam("user_pw") String userpw, HttpSession session) {
		Integer logged = jdbctest.login(userid, userpw);
		if( logged == 1 ) {
			session.setAttribute("loggedInUser", userid); // 세션에 로그인 사용자 정보 저장
            session.setMaxInactiveInterval(30 * 60); // 30분 동안 비활성 시 세션 만료
		}
		return logged;
	}
	
	// 로그아웃 처리
    @PostMapping("/logout")
    public Integer logout(HttpSession session) {
        session.invalidate(); // 세션 무효화
        return 1;
    }
}