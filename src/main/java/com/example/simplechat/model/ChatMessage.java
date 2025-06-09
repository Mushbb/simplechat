package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {
	private String id;
	private String chat;
	private int messageNum;
	
	public ChatMessage(){ }
	public ChatMessage(String newId, String newChat, int newNum){ 
		id = newId;
		chat = newChat;
		messageNum = newNum;
	}
}