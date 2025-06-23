package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Setter
@Getter
public class ChatMessage {
	private String id;		// userid
	private String name;	// username: 시간에 따라 변함
	private String chat;
	private int messageNum;	// message counter
	private static int messageCounter = 0;
	private String timestamp;
	
	public ChatMessage(){ }
	
	// Primary constructor: This is where the common logic resides.
	public ChatMessage(String newId, String newName, String newChat, int num){
		this.id = newId;
		this.name = newName;
		this.chat = newChat;
		this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm"));
		this.messageNum = num;
	}
	
	// Calls the primary constructor, then handles messageCounter increment
	public ChatMessage(String newId, String newName, String newChat){
		this(newId, newName, newChat, messageCounter++); // Call the constructor with all parameters
	}
	
	// Calls the primary constructor, then conditionally increments messageCounter
	public ChatMessage(String newId, String newName, String newChat, boolean counter){
		this(newId, newName, newChat, messageCounter); // Call the constructor with all parameters
		messageCounter += counter ? 1 : 0;
	}
}