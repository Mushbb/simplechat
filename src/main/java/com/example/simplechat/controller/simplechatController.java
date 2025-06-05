package com.example.simplechat.controller;

import com.example.simplechat.service.SimplechatService;
import com.example.simplechat.model.ChatMessage;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
public class simplechatController {
	// 1. 서비스 객체를 참조할 필드 선언 (불변성을 위해 final로 선언)
	private final SimplechatService serv;
	
	// 2. 생성자를 통한 의존성 주입 (가장 권장되는 방법)
    // 스프링이 UserService 타입의 빈(객체)을 찾아서 이 생성자의 파라미터로 주입해 줍니다.
	public simplechatController(SimplechatService serv) {
		this.serv = serv;
	}
	
	@GetMapping("/")
	public Flux<ChatMessage> catchGetRequests(HttpServletRequest request) {
        //String requestURI = request.getRequestURI();
        
        return serv.getChat();
    }
	@GetMapping("/init")
	public Flux<ChatMessage> catchAllGetRequests(HttpServletRequest request) {
        //String requestURI = request.getRequestURI();
        
        return serv.getAllChat();
    }
	
	@PostMapping("/")
	public Mono<Void> recvMessage(@RequestParam("message") String request) {
		ChatMessage temp = new ChatMessage();
		temp.setId("client");
		temp.setChat(request);
		temp.setMessageNum(serv.getChatSize());
		
		Mono<ChatMessage> msgmono = Mono.just(temp);
		
		return serv.addChat(msgmono).then();
	}
	
	
}