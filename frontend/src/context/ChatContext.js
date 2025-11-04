import React, { createContext, useState, useContext, useRef, useEffect, useCallback } from 'react';
import { AuthContext } from './AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = axiosInstance.getUri();

const ChatContext = createContext();

function ChatProvider({ children }) {
    const { user, loading, forceLogout } = useContext(AuthContext);
    
    const [joinedRooms, setJoinedRooms] = useState([]);
    const [activeRoomId, setActiveRoomId] = useState(null);
    const [messagesByRoom, setMessagesByRoom] = useState({});
    const [usersByRoom, setUsersByRoom] = useState({});
    const [isRoomLoading, setIsRoomLoading] = useState({});
    const [unreadRooms, setUnreadRooms] = useState(new Set());
    const [hasMoreMessagesByRoom, setHasMoreMessagesByRoom] = useState({});
    
    const stompClientsRef = useRef(new Map());
    const activeRoomIdRef = useRef(activeRoomId);
    const joiningRoomRef = useRef(null);
    
    useEffect(() => {
        activeRoomIdRef.current = activeRoomId;
    }, [activeRoomId]);
    
    const joinRoomAndConnect = useCallback(async (newRoom) => {
        // 이미 참여한 방인지 먼저 확인 (이것은 중복 탭 생성과는 다른 문제입니다)
        const isAlreadyMember = joinedRooms.some(room => room.id === newRoom.id);
        if (isAlreadyMember) {
            console.log(`[ChatContext] 이미 참여한 방 #${newRoom.id} 입니다.`);
            return;
        }
        
        // "잠금 장치"를 확인합니다. 다른 곳에서 이미 이 방에 참여하는 중이라면, 중복 실행을 막습니다.
        if (joiningRoomRef.current === newRoom.id) {
            console.warn(`[ChatContext] #${newRoom.id} 방에 이미 참여하는 작업이 진행 중입니다.`);
            return;
        }
        
        try {
            // ✨ 3. 참여 작업을 시작하기 직전에 "잠금"을 겁니다.
            joiningRoomRef.current = newRoom.id;
            
            // 웹소켓 연결 및 데이터 로딩을 기다립니다.
            await connectToRoom(newRoom.id);
            
            // 모든 작업이 성공적으로 끝나면, joinedRooms 상태를 안전하게 업데이트합니다.
            setJoinedRooms(prevRooms => {
                // 혹시 모를 중복을 한 번 더 확인하고 추가합니다.
                if (prevRooms.some(room => room.id === newRoom.id)) {
                    return prevRooms;
                }
                return [...prevRooms, newRoom];
            });
            
        } catch (error) {
            console.error(`[ChatContext] #${newRoom.id} 방 참여 실패:`, error);
            throw error; // 실패했음을 LobbyPage에 알려줍니다.
        } finally {
            // ✨ 4. 작업이 성공하든 실패하든, 마지막에는 반드시 "잠금"을 해제합니다.
            joiningRoomRef.current = null;
        }
    }, [joinedRooms]); // LobbyPage에서 최신 joinedRooms를 참조해야 하므로 의존성 배열 유지
        
    useEffect(() => {
        const setupConnections = async () => {
            if (!loading && user) {
                // Connect to chat rooms
                try {
                    const response = await axiosInstance.get('/api/my-rooms');
                    const myRooms = response.data;
                    setJoinedRooms(myRooms);
                    myRooms.forEach(room => connectToRoom(room.id));
                } catch (error) {
                    console.error("내 채팅방 목록 가져오기 실패:", error);
                }
                
            } else if (!loading && !user) {
                // Cleanup on logout
                stompClientsRef.current.forEach(client => client.deactivate());
                stompClientsRef.current.clear();
                setJoinedRooms([]);
                setMessagesByRoom({});
                setUsersByRoom({});
            }
        };
        setupConnections();
        
        return () => {
            // Cleanup function
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
            setJoinedRooms([]);
            setMessagesByRoom({});
            setUsersByRoom({});
        };
    }, [user, loading]);
    
    const onMoreMessagesReceived = (response) => {
        const { roomId, messages } = response;
        if (!roomId || !messages) return;
        
        // ✅ NEW: 서버가 빈 배열을 반환하면, 더 이상 메시지가 없다고 기록합니다.
        if (messages.length === 0) {
            setHasMoreMessagesByRoom(prev => ({ ...prev, [roomId]: false }));
            return;
        }
        
        // 더 이상 불러올 메시지가 없을 때(빈 배열 수신) 로딩 상태를 중지시키기 위함
        if (messages.length === 0) {
            console.log(`No more messages for room #${roomId}`);
            // ChatPage에서 isFetchingMore를 false로 바꿔주도록 빈 배열을 전달
        }
        
        setMessagesByRoom(prev => {
            const currentMessages = prev[roomId] || [];
            const existingMessageIds = new Set(currentMessages.map(msg => msg.messageId));
            const newMessages = messages.filter(msg => !existingMessageIds.has(msg.messageId));
            
            if (newMessages.length === 0) return prev; // 중복 또는 빈 배열이면 상태 변경 안함
            
            return { ...prev, [roomId]: [...newMessages, ...currentMessages] };
        });
    };
    
    const connectToRoom = (roomId) => {
        return new Promise((resolve, reject) => {
            if (!user || stompClientsRef.current.has(roomId)) return;
            
            setIsRoomLoading(prev => ({ ...prev, [roomId]: true }));
            
            const client = new Client({
                webSocketFactory: () => new SockJS(`${SERVER_URL}/ws`),
                connectHeaders: { user_id: String(user.userId), room_id: String(roomId) },
                reconnectDelay: 0,
                onConnect: options => {
                    console.log(`Room #${roomId}: 웹소켓 연결 성공`);
                    // ✅ FIX: @SendToUser가 보내는 이전 메시지 목록을 수신하기 위한 구독
                    client.subscribe('/user/topic/queue/reply', (payload) => onMoreMessagesReceived(JSON.parse(payload.body)));
                    client.subscribe(`/topic/${roomId}/public`, (payload) => onMessageReceived(roomId, payload));
                    client.subscribe(`/topic/${roomId}/users`, (payload) => onUserInfoReceived(roomId, payload));
                    client.subscribe(`/topic/${roomId}/previews`, (payload) => onPreviewReceived(roomId, payload));
                    
                    axiosInstance.get(`/room/${roomId}/init?lines=20`).then(response => {
                        const data = response.data;
                        setUsersByRoom(prev => ({ ...prev, [roomId]: data.users || [] }));
                        setMessagesByRoom(prev => ({ ...prev, [roomId]: [...(data.messages || [])].reverse() }));
                        
                        // ✅ NEW: 방에 처음 입장할 때는 기본적으로 더 불러올 메시지가 있다고 가정합니다.
                        // (만약 처음부터 메시지가 20개 미만이면 false로 설정하는 최적화도 가능합니다.)
                        const initialMessages = response.data.messages || [];
                        setHasMoreMessagesByRoom(prev => ({ ...prev, [roomId]: initialMessages.length >= 20 }));
                        resolve(); // 성공 신호!
                    }).catch(error => {
                        console.error(`#${roomId} 방 초기화 데이터 로딩 실패:`, error);
                        reject(error); // 실패 신호!
                    }).finally(() => {
                        // ✅ 3. API 호출이 성공하든 실패하든 끝나면 로딩 상태를 false로 설정
                        setIsRoomLoading(prev => ({ ...prev, [roomId]: false }));
                    });
                }
            });
            client.onDisconnect = () => {
                console.error(`Room #${roomId}: 웹소켓 연결이 끊어졌습니다.`);
                forceLogout();
            };
            
            client.onStompError = (frame) => {
                console.error('STOMP Error:', frame.headers['message'], frame.body);
                forceLogout();
                reject(new Error(frame.headers['message']));
            };
            
            client.onWebSocketError = (error) => {
                console.error('WebSocket Error:', error);
                forceLogout();
            };
            
            client.activate();
            stompClientsRef.current.set(roomId, client);
        });
    };
    
    const onMessageReceived = (roomId, payload) => {
        // 메시지를 받았을 때, 현재 보고 있는 방이 아니면 '안 읽은 방'으로 추가
        if (roomId !== activeRoomIdRef.current) {
            setUnreadRooms(prev => new Set(prev).add(roomId));
        }
        
        const message = JSON.parse(payload.body);
        setMessagesByRoom(prev => {
            const updatedMessages = [...(prev[roomId] || []), message];
            return { ...prev, [roomId]: updatedMessages };
        });
        // 프로필 사진 업데이트 로직
        setUsersByRoom(prev => {
            const currentUsers = prev[roomId] || [];
            const userIndex = currentUsers.findIndex(u => u.userId === message.authorId);
            if (userIndex > -1 && currentUsers[userIndex].profileImageUrl !== message.authorProfileImageUrl) {
                const newUsers = [...currentUsers];
                newUsers[userIndex] = { ...newUsers[userIndex], profileImageUrl: message.authorProfileImageUrl };
                return { ...prev, [roomId]: newUsers };
            }
            return prev;
        });
    };
    
    // 사용자가 방에 입장(탭 클릭)하면 '안 읽은' 상태를 해제
    useEffect(() => {
        // activeRoomId가 있고, 해당 방이 unreadRooms Set에 있다면
        if (activeRoomId && unreadRooms.has(activeRoomId)) {
            // unreadRooms Set에서 현재 방 ID를 제거하여 '읽음' 처리
            setUnreadRooms(prev => {
                const newSet = new Set(prev);
                newSet.delete(activeRoomId);
                return newSet;
            });
        }
    }, [activeRoomId, unreadRooms]); // activeRoomId가 바뀔 때마다 실행됩니다.
    
    const onUserInfoReceived = (roomId, payload) => {
        const userEvent = JSON.parse(payload.body);
        setUsersByRoom(prev => {
            const currentUsers = prev[roomId] || [];
            const userIndex = currentUsers.findIndex(u => u.userId === userEvent.userId);
            let newUsers = [...currentUsers];
            switch (userEvent.eventType) {
                case 'ENTER':
                case 'ROOM_IN':
                    if (userIndex === -1) {
                        newUsers.push({
                            userId: userEvent.userId,
                            nickname: userEvent.nickname,
                            conn: 'CONNECT',
                            profileImageUrl: userEvent.profileImageUrl,
                            role: userEvent.role // 역할 정보 추가
                        });
                    } else {
                        newUsers[userIndex].conn = 'CONNECT';
                    }
                    break;
                case 'EXIT':
                    if (userIndex !== -1) newUsers[userIndex].conn = 'DISCONNECT';
                    break;
                case 'ROOM_OUT':
                case 'ROOM_DELETED':
                    newUsers = currentUsers.filter(u => u.userId !== userEvent.userId);
                    break;
                case 'NICK_CHANGE':
                    if (userIndex !== -1) newUsers[userIndex] = { ...newUsers[userIndex], nickname: userEvent.nickname };
                    break;
            }
            return { ...prev, [roomId]: newUsers };
        });
    };
    
    const onPreviewReceived = (roomId, payload) => {
        const preview = JSON.parse(payload.body);
        setMessagesByRoom(prev => {
            const currentMessages = prev[roomId] || [];
            const updatedMessages = currentMessages.map(msg =>
                msg.messageId === preview.messageId ? { ...msg, linkPreview: preview } : msg
            );
            return { ...prev, [roomId]: updatedMessages };
        });
    };
    
    const initializeChat = async () => {
        // AuthContext의 user 상태를 직접 참조합니다.
        if (!user) {
            console.log("사용자 정보가 없어 채팅을 초기화할 수 없습니다.");
            return;
        }
        try {
            // 기존 연결이 있다면 모두 해제하고 시작합니다.
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
            
            const response = await axiosInstance.get(`${SERVER_URL}/api/my-rooms`);
            const myRooms = response.data;
            setJoinedRooms(myRooms);
            myRooms.forEach(room => connectToRoom(room.id));
        } catch (error) {
            console.error("ChatContext 초기화 실패:", error);
        }
    };
    
    // ✅ NEW: 이전 메시지를 서버에 요청하는 함수
    const loadMoreMessages = (roomId) => {
        const client = stompClientsRef.current.get(roomId);
        const currentMessages = messagesByRoom[roomId] || [];
        
        // 클라이언트가 연결되지 않았거나, 메시지가 없으면 요청하지 않음
        if (!client?.connected || currentMessages.length === 0) {
            console.log("Cannot load more messages. Client not connected or no existing messages.");
            return;
        }
        
        const oldestMessage = currentMessages[0]; // 배열의 첫 번째 메시지가 가장 오래된 메시지
        
        const requestDto = {
            roomId: roomId,
            beginId: oldestMessage.messageId,
            rowCount: 20 // 한 번에 20개씩 불러오기
        };
        
        // 백엔드의 getMessageList 메시지 핸들러로 요청 전송
        client.publish({
            destination: '/app/chat.getMessageList',
            body: JSON.stringify(requestDto),
        });
    };
    
    // ✅ 3. 방 나가기 함수 추가
    const exitRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}/users`);
            // 상태 업데이트: 나간 방을 joinedRooms 목록에서 제거
            setJoinedRooms(prev => prev.filter(room => room.id !== roomId));
            // 웹소켓 연결 해제
            stompClientsRef.current.get(roomId)?.deactivate();
            stompClientsRef.current.delete(roomId);
        } catch (error) {
            console.error("Failed to exit room:", error);
            alert(error.response?.data?.message || "방에서 나가는 데 실패했습니다.");
        }
    };
    
    // ✅ 4. 방 삭제 함수 추가
    const deleteRoom = async (roomId) => {
        try {
            await axiosInstance.delete(`/room/${roomId}`);
            setJoinedRooms(prev => prev.filter(room => room.id !== roomId));
            stompClientsRef.current.get(roomId)?.deactivate();
            stompClientsRef.current.delete(roomId);
        } catch (error) {
            console.error("Failed to delete room:", error);
            alert(error.response?.data?.message || "방 삭제에 실패했습니다.");
        }
    };
    
    const value = {
        joinedRooms,
        activeRoomId,
        setActiveRoomId,
        messagesByRoom,
        usersByRoom,
        stompClientsRef,
        initializeChat,
        isRoomLoading,
        joinRoomAndConnect,
        exitRoom,
        deleteRoom,
        unreadRooms,
        hasMoreMessagesByRoom,
        loadMoreMessages,
    };
    
    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export { ChatContext, ChatProvider };