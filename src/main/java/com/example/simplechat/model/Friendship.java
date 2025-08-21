package com.example.simplechat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    private long userId1;
    private long userId2;
    private Status status;
    private LocalDateTime createdAt;
    private long relationId;
    
    public static enum Status {
    	PENDING, ACCEPTED, BLOCKED
	}
}
