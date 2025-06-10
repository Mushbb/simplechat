package com.example.simplechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Setter
@Getter
public class ChatRoom {
	private String name;
	private String id;	// for controller
	private int pops;	// population
	
    private final List<ChatMessage> chats = new ArrayList<>(); // final로 선언하고 바로 초기화
    private final Map<Integer, UserInfo> users = new HashMap<>();
    
    public ChatRoom(String newName) { name = newName; }
    
    
}