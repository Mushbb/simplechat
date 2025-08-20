import React, { createContext, useState, useContext, useRef, useEffect } from 'react';
import { AuthContext } from './AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = 'http://10.50.131.25:8080';

const ChatContext = createContext();

function ChatProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const [joinedRooms, setJoinedRooms] = useState([]);
    const [activeRoomId, setActiveRoomId] = useState(null);
    const [messagesByRoom, setMessagesByRoom] = useState({});
    const [usersByRoom, setUsersByRoom] = useState({});
    const [isRoomLoading, setIsRoomLoading] = useState({});
    const [unreadRooms, setUnreadRooms] = useState(new Set());  // 안 읽은 메시지가 있는 방 ID를 저장할 Set state
    // ✅ NEW: 방마다 더 불러올 메시지가 있는지 추적하는 state
    const [hasMoreMessagesByRoom, setHasMoreMessagesByRoom] = useState({});
    
    const stompClientsRef = useRef(new Map());
    const activeRoomIdRef = useRef(activeRoomId);
    // ✅ NEW: 개인 응답 채널의 구독 상태를 관리할 ref를 추가합니다.
    const replySubscriptionRef = useRef(null);
    
    // activeRoomId state가 바뀔 때마다 '전자 게시판(ref)'의 내용을 즉시 업데이트
    useEffect(() => {
        activeRoomIdRef.current = activeRoomId;
    }, [activeRoomId]);
    
    const joinRoomAndConnect = (newRoom) => {
        // 이미 명단에 있으면 아무것도 하지 않음
        if (joinedRooms.some(room => room.id === newRoom.id)) {
            console.log(`Room #${newRoom.id} is already in the list.`);
            return;
        }
        
        // 1. lifeguard의 명단(joinedRooms state)에 새로운 방을 추가
        setJoinedRooms(prevRooms => [...prevRooms, newRoom]);
        
        // 2. 해당 방의 웹소켓 연결 및 초기화 시작
        connectToRoom(newRoom.id);
    };
    
    useEffect(() => {
        const connectToAllMyRooms = async () => {
            if (!loading && user) {
                try {
                    const response = await axiosInstance.get('/api/my-rooms');
                    const myRooms = response.data;
                    setJoinedRooms(myRooms);
                    myRooms.forEach(room => connectToRoom(room.id));
                } catch (error) {
                    console.error("내 채팅방 목록 가져오기 실패:", error);
                }
            } else if (!loading && !user) {
                // ✅ 로그아웃 시 또는 비로그인 상태가 확정되었을 때 모든 연결을 정리합니다.
                stompClientsRef.current.forEach(client => client.deactivate());
                stompClientsRef.current.clear();
                setJoinedRooms([]);
                setMessagesByRoom({});
                setUsersByRoom({});
            }
        };
        connectToAllMyRooms();

        return () => {
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
            setJoinedRooms([]);
            setMessagesByRoom({});
            setUsersByRoom({});
            replySubscriptionRef.current = null; // 구독 상태 초기화
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
        if (!user || stompClientsRef.current.has(roomId)) return;
        
        setIsRoomLoading(prev => ({ ...prev, [roomId]: true }));
        
        const client = new Client({
            webSocketFactory: () => new SockJS(`${SERVER_URL}/ws`),
            connectHeaders: { user_id: String(user.userId), room_id: String(roomId) },
            onConnect: options => {
                console.log(`Room #${roomId}: 웹소켓 연결 성공`);
                client.subscribe(`/topic/${roomId}/public`, (payload) => onMessageReceived(roomId, payload));
                client.subscribe(`/topic/${roomId}/users`, (payload) => onUserInfoReceived(roomId, payload));
                client.subscribe(`/topic/${roomId}/previews`, (payload) => onPreviewReceived(roomId, payload));
                // ✅ MODIFIED: 아직 구독하지 않았을 때만 개인 응답 채널을 구독합니다.
                if (!replySubscriptionRef.current) {
                    console.log("Subscribing to the user-reply topic for the first time.");
                    replySubscriptionRef.current = client.subscribe(`/user/topic/queue/reply`, (payload) => {
                        console.log("Received a message on user-reply topic.");
                        const response = JSON.parse(payload.body);
                        onMoreMessagesReceived(response);
                    });
                }
                // 알림 채널은 여기에 추가할 수 있습니다.

                axiosInstance.get(`/room/${roomId}/init?lines=20`).then(response => {
                    const data = response.data;
                    setUsersByRoom(prev => ({ ...prev, [roomId]: data.users || [] }));
                    setMessagesByRoom(prev => ({ ...prev, [roomId]: [...(data.messages || [])].reverse() }));
                    
                    // ✅ NEW: 방에 처음 입장할 때는 기본적으로 더 불러올 메시지가 있다고 가정합니다.
                    // (만약 처음부터 메시지가 20개 미만이면 false로 설정하는 최적화도 가능합니다.)
                    const initialMessages = response.data.messages || [];
                    setHasMoreMessagesByRoom(prev => ({ ...prev, [roomId]: initialMessages.length >= 20 }));
                    
                }).finally(() => {
                    // ✅ 3. API 호출이 성공하든 실패하든 끝나면 로딩 상태를 false로 설정
                    setIsRoomLoading(prev => ({ ...prev, [roomId]: false }));
                });
            },
        });

        client.activate();
        stompClientsRef.current.set(roomId, client);
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
            // 상태 업데이트: 삭제된 방을 joinedRooms 목록에서 제거
            setJoinedRooms(prev => prev.filter(room => room.id !== roomId));
            // 웹소켓 연결 해제
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
        hasMoreMessagesByRoom, // ✅ NEW: ChatPage에서 사용할 수 있도록 추가
        loadMoreMessages,
    };

    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export { ChatContext, ChatProvider };