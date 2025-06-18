package com.example.simplechat.model;

import com.example.simplechat.event.ChatMessageAddedToRoomEvent; // 새로 정의한 이벤트 import
import org.springframework.context.ApplicationEventPublisher; // 이벤트 발행자 import

import java.util.concurrent.CopyOnWriteArrayList;
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
	
    private final List<ChatMessage> chats = new CopyOnWriteArrayList<>();
    private final Map<Integer, UserInfo> users = new HashMap<>();
    private final UserInfo admin = new UserInfo(-1, "Server" );
    
    // Spring이 이벤트를 발행할 수 있도록 ApplicationEventPublisher를 주입받습니다.
    // ChatRoom은 일반적으로 @Component가 아니므로, 외부에서 주입해줘야 합니다.
    // ChatService에서 ChatRoom을 생성/관리하면서 주입해주는 방식이 일반적입니다.
    private ApplicationEventPublisher eventPublisher;
    
    
    public ChatRoom(String newName) { name = newName; }
    
    // Spring이 ChatRoom 객체에 eventPublisher를 주입할 수 있도록 setter를 제공합니다.
    // 이 setter는 ChatService에서 ChatRoom을 생성한 후 호출될 수 있습니다.
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    
    
    public void addChat(ChatMessage chat) { 
    	chats.add(chat);
    	
    	// 메시지가 성공적으로 추가된 후 이벤트 발행
        if (eventPublisher != null) {
            // 이벤트를 발생시킨 소스(source)로 'this' (현재 ChatRoom 인스턴스)를 전달
            eventPublisher.publishEvent(new ChatMessageAddedToRoomEvent(this, chat, name));
            System.out.println("ChatRoom[" + name + "]: ChatMessageAddedToRoomEvent 발행됨.");
        } else {
            System.err.println("ChatRoom[" + name + "]: EventPublisher가 주입되지 않아 이벤트를 발행할 수 없습니다.");
        }
	}
    public void addChat(String id, String nick, String str) { addChat(new ChatMessage(id, nick, str));	}
    public void addChat(Integer id, String nick, String str) { addChat(new ChatMessage(""+id, nick, str)); 	}
    
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