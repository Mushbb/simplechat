package com.example.simplechat.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserInfo {
	private String username;
	
	public UserInfo( String Name ) { username = Name; }
}