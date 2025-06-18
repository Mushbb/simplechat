package com.example.simplechat.model;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserInfo {
	private String username;
	private int id;
	private static final AtomicInteger nextId = new AtomicInteger(1);	// automatic id counter
	
	public UserInfo( String Name ) { username = Name; id = nextId.getAndIncrement(); }
	public UserInfo( Integer newId, String Name ) { username = Name; id = newId; }
}