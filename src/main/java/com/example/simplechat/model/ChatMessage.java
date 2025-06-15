package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {
	private String id;		// userid
	private String name;	// username: 시간에 따라 변함
	private String chat;
	private int messageNum;	// message counter
	private static int messageCounter = 0;
	
	public ChatMessage(){ }
	public ChatMessage(String newId, String newName, String newChat){ 
		id = newId;
		chat = newChat;
		name = newName;
		messageNum = messageCounter++;
	}
	
	public ChatMessage(String newId, String newName, String newChat, int num){ 
		id = newId;
		chat = newChat;
		name = newName;
		messageNum = num;
	}
	
	public ChatMessage(String newId, String newName, String newChat, boolean counter){ 
		id = newId;
		chat = newChat;
		name = newName;
		messageNum = messageCounter;
		messageCounter += counter ? 1 : 0;
	}
	
}