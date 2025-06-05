package com.example.simplechat.service;

import org.springframework.stereotype.Service; 
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import jakarta.annotation.PreDestroy;

import com.example.simplechat.model.ChatMessage;

@Service
@EnableScheduling
public class SimplechatService {
	private List<ChatMessage> chats;
	private String text;
	private int flag;
	private Scanner sc;
	
	public SimplechatService() {
		chats = new ArrayList<>();
		text = "initialized";
		flag = 1;
		sc = new Scanner(System.in);
	}
	
	@Scheduled(fixedRate = 1000)
	public void serverChat() {
		if( flag == 0 ) return;	// already scanning
		flag = 0;
		
		text = sc.nextLine();
		ChatMessage msg = new ChatMessage();
		msg.setId("server");
		msg.setChat(text);
		chats.add(msg);
		
		flag = 1;
	}
	
	public String getText() { return this.text;	}
	public void setText(String str) { this.text = str; }
	public void addChat(String id, String str) {
		ChatMessage msg = new ChatMessage();
		msg.setId(id);
		msg.setChat(str);
		chats.add(msg);
	}
	public List<ChatMessage> getChat(){ return this.chats; }
	
	@PreDestroy
	public void closeScanner() { sc.close(); }
}