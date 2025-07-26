package com.example.simplechat.service;

import org.springframework.stereotype.Component; 
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomSessionManager {
	private record SessionInfo(Long roomId, Long userId) { }
	
	// Key: roomId, Value: Map<userId, sessionId>
	private final Map<Long, Map<Long, String>> sessionsByRoom = new ConcurrentHashMap<>();
	
	// Key: sessionId, Value: Map<roomId, userId>
	private final Map<String, SessionInfo> sessionInfoById = new ConcurrentHashMap<>();
	
	
	/**
	 * 새로운 사용자 세션을 등록합니다.
	 * @param roomId 사용자가 접속한 방 ID
	 * @param userId 사용자 ID
	 * @param sessionId 웹소켓 세션 ID
	 */
	public void registerSession(Long roomId, Long userId, String sessionId) {
	    // 방별 세션 맵에 등록
	    sessionsByRoom.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, sessionId);
	    // 역방향 맵에 등록
	    sessionInfoById.put(sessionId, new SessionInfo (roomId, userId));
	    System.out.println("[SessionManager] Registered: Room=" + roomId + ", User=" + userId + ", Session=" + sessionId);
	}

	/**
	 * 세션 연결 종료 시 세션을 제거합니다.
	 * @param sessionId 연결 종료된 웹소켓 세션 ID
	 */
	public void unregisterSession(String sessionId) {
		SessionInfo info = sessionInfoById.remove(sessionId);
	    if (info != null) {
	        Map<Long, String> usersInRoom = sessionsByRoom.get(info.roomId());
	        if (usersInRoom != null) {
	            usersInRoom.remove(info.userId());
	            // 방에 아무도 없으면 방 자체를 맵에서 제거 (메모리 관리)
	            if (usersInRoom.isEmpty()) {
	                sessionsByRoom.remove(info.roomId());
	            }
	        }
	        System.out.println("[SessionManager] Unregistered: Room=" + info.roomId() + ", User=" + info.userId() + ", Session=" + sessionId);
	    }
	}

	/**
	 * 특정 방에 현재 접속 중인 모든 사용자의 ID Set을
	 * 반환합니다.
	 * @param roomId 방 ID
	 * @return 접속 중인 사용자 ID의 Set
	 */
	public Set<Long> getConnectedUsers(Long roomId) {
	    return sessionsByRoom.getOrDefault(roomId, Map.of()).keySet();
	}

	/**
	 * 특정 사용자가 특정 방에 접속 중인지 확인합니다.
	 * @param roomId 방 ID
	 * @param userId 사용자 ID
	 * @return 접속 중이면 true, 아니면 false
	 */
	public boolean isUserConnected(Long roomId, Long
	        userId) {
	    return sessionsByRoom.getOrDefault(roomId, Map.of()).containsKey(userId);
	}

	/**
	 * 특정 사용자의 현재 세션 ID를 반환합니다. (DM
	 * 기능에 사용)
	 * @param userId 사용자 ID
	 * @return 세션 ID 문자열, 접속 중이 아니면 null
	 */
	public String getSessionId(Long userId) {
	    // 모든 방을 순회하며 해당 userId를 찾아야 함.
	    // 사용자가 여러 방에 동시에 접속하지 않는다는 가정 하에 동작.
	    for (Map<Long, String> usersInRoom : sessionsByRoom.values()) {
	        for (Map.Entry<Long, String> entry : usersInRoom.entrySet()) {
	            if (entry.getKey().equals(userId)) {
	                return entry.getValue();
	            }
	        }
	    }
	    return null;
	}
}
