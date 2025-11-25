package com.example.simplechat.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; 

/**
 * 채팅방 및 사용자 세션을 관리하는 컴포넌트입니다.
 * 각 채팅방에 접속한 사용자들의 세션 정보를 추적하고, 세션 ID와 사용자 ID 간의 매핑을 유지합니다.
 * 이를 통해 특정 방에 접속한 사용자 목록 조회, 특정 사용자의 접속 여부 확인 등의 기능을 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class RoomSessionManager {

	private static final Logger logger = LoggerFactory.getLogger(RoomSessionManager.class);

	/**
	 * 세션 정보를 저장하는 내부 레코드 클래스입니다.
	 *
	 * @param roomId 사용자가 접속한 방의 ID
	 * @param userId 사용자의 ID
	 */
	private record SessionInfo(Long roomId, Long userId) { }
	
	// Key: roomId, Value: Map<userId, sessionId>
	private final Map<Long, Map<Long, String>> sessionsByRoom = new ConcurrentHashMap<>();
	
	// Key: sessionId, Value: SessionInfo (roomId, userId)
	private final Map<String, SessionInfo> sessionInfoById = new ConcurrentHashMap<>();
	
	
	/**
	 * 새로운 사용자 세션을 등록합니다.
	 * 사용자가 채팅방에 접속했을 때 호출됩니다.
	 *
	 * @param roomId 사용자가 접속한 방 ID
	 * @param userId 사용자 ID
	 * @param sessionId 웹소켓 세션 ID
	 */
	public void registerSession(Long roomId, Long userId, String sessionId) {
	    sessionsByRoom.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, sessionId);
	    sessionInfoById.put(sessionId, new SessionInfo (roomId, userId));
	    logger.info("[SessionManager] 세션 등록됨: 방 ID={} 사용자 ID={} 세션 ID={}", roomId, userId, sessionId);
	}

	/**
	 * 세션 연결 종료 시 세션을 제거합니다.
	 * 사용자가 채팅방에서 나갔을 때 호출됩니다.
	 *
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
	        logger.info("[SessionManager] 세션 해제됨: 방 ID={} 사용자 ID={} 세션 ID={}", info.roomId(), info.userId(), sessionId);
	    }
	}

	/**
	 * 특정 방에 현재 접속 중인 모든 사용자의 ID Set을 반환합니다.
	 *
	 * @param roomId 방 ID
	 * @return 접속 중인 사용자 ID의 Set. 접속자가 없으면 빈 Set을 반환합니다.
	 */
	public Set<Long> getConnectedUsers(Long roomId) {
	    return sessionsByRoom.getOrDefault(roomId, Map.of()).keySet();
	}

	/**
	 * 특정 사용자가 특정 방에 접속 중인지 확인합니다.
	 *
	 * @param roomId 방 ID
	 * @param userId 사용자 ID
	 * @return 접속 중이면 true, 아니면 false
	 */
	public boolean isUserConnected(Long roomId, Long userId) {
	    return sessionsByRoom.getOrDefault(roomId, Map.of()).containsKey(userId);
	}

	/**
	 * 특정 사용자의 현재 세션 ID를 반환합니다.
	 * 사용자가 여러 방에 동시에 접속하지 않는다는 가정 하에 효율적으로 동작합니다.
	 * 주로 DM (Direct Message) 기능 구현에 사용될 수 있습니다.
	 *
	 * @param userId 사용자 ID
	 * @return 세션 ID 문자열, 접속 중이 아니면 null
	 */
	public String getSessionId(Long userId) {
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
