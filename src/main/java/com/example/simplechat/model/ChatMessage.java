package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {
	private String id;		// userid
	private String chat;
	private int messageNum;	// message counter
	private static int messageCounter = 0;
	
	public ChatMessage(){ }
	public ChatMessage(String newId, String newChat){ 
		id = newId;
		chat = newChat;
		messageNum = messageCounter++;
	}
	
	public ChatMessage(Integer newId, String newChat){ 
		id = ""+newId;
		chat = newChat;
		messageNum = messageCounter++;
	}
	
	public ChatMessage(String newId, String newChat, int num){ 
		id = newId;
		chat = newChat;
		messageNum = num;
	}
	
	public ChatMessage(Integer newId, String newChat, int num){ 
		id = ""+newId;
		chat = newChat;
		messageNum = num;
	}
	
	public ChatMessage(String newId, String newChat, boolean counter){ 
		id = newId;
		chat = newChat;
		messageNum = messageCounter;
		messageCounter += counter ? 1 : 0;
	}
	public ChatMessage(Integer newId, String newChat, boolean counter){ 
		id = newId.toString();
		chat = newChat;
		messageNum = messageCounter;
		messageCounter += counter ? 1 : 0;
	}
	
}