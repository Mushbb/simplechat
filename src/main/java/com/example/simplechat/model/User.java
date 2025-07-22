package com.example.simplechat.model;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class User {
	private String username;
	private int id;
	private static final AtomicInteger nextId = new AtomicInteger(1);	// automatic id counter
	
	public User( String Name ) { username = Name; id = nextId.getAndIncrement(); }
	public User( Integer newId, String Name ) { username = Name; id = newId; }
}