package com.example.simplechat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRoom {
	private String name;
	private String id;	// for controller
	
    private final List<ChatMessage> chats = new ArrayList<>();
    private final Map<Integer, UserInfo> users = new HashMap<>();
    private final UserInfo admin = new UserInfo(-1, "Server" );
    
    public ChatRoom(String newName) { name = newName; }
    
    public void addChat(ChatMessage chat) { chats.add(chat); }
    public void addChat(String id, String nick, String str) { chats.add(new ChatMessage(id, nick, str)); }
    public void addChat(Integer id, String nick, String str) { 
    	chats.add(new ChatMessage(""+id, nick, str)); 
    	}
    
    public int addUser(UserInfo user) { 
    	users.put(user.getId(), user);
    	return users.size(); 
	}
    public int getPopsCount() { return users.size(); }
    public ChatMessage getLastChat() { return chats.getLast(); }
    public UserInfo getPop(Integer key) { return users.get(key); }
    public UserInfo getPop(String key) { return users.get(Integer.parseInt(key)); }
    
    public void ChangeNick(String id, String nick) { 
    	users.get(Integer.parseInt(id)).setUsername(nick);
    }
}