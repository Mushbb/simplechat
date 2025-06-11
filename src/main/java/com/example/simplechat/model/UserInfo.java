package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserInfo {
	private String username;
	private int id;
	private static int nextId = 0;	// automatic id counter
	
	public UserInfo( String Name ) { username = Name; id = nextId++; }
	public UserInfo( Integer newId, String Name ) { username = Name; id = newId; }
}