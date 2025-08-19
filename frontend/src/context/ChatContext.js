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

    const stompClientsRef = useRef(new Map());

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
        };
    }, [user, loading]);

    const connectToRoom = (roomId) => {
        if (!user || stompClientsRef.current.has(roomId)) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(`${SERVER_URL}/ws`),
            connectHeaders: { user_id: String(user.userId), room_id: String(roomId) },
            onConnect: () => {
                console.log(`Room #${roomId}: 웹소켓 연결 성공`);
                client.subscribe(`${SERVER_URL}/topic/${roomId}/public`, (payload) => onMessageReceived(roomId, payload));
                client.subscribe(`${SERVER_URL}/topic/${roomId}/users`, (payload) => onUserInfoReceived(roomId, payload));
                client.subscribe(`${SERVER_URL}/topic/${roomId}/previews`, (payload) => onPreviewReceived(roomId, payload));
                // 알림 채널은 여기에 추가할 수 있습니다.

                axiosInstance.get(`${SERVER_URL}/room/${roomId}/init?lines=20`).then(response => {
                    const data = response.data;
                    setUsersByRoom(prev => ({ ...prev, [roomId]: data.users || [] }));
                    setMessagesByRoom(prev => ({ ...prev, [roomId]: [...(data.messages || [])].reverse() }));
                });
            },
        });

        client.activate();
        stompClientsRef.current.set(roomId, client);
    };

    const onMessageReceived = (roomId, payload) => {
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

    const onUserInfoReceived = (roomId, payload) => {
        const userEvent = JSON.parse(payload.body);
        setUsersByRoom(prev => {
            const currentUsers = prev[roomId] || [];
            const userIndex = currentUsers.findIndex(u => u.userId === userEvent.userId);
            let newUsers = [...currentUsers];
            switch (userEvent.eventType) {
                case 'ENTER':
                    if (userIndex === -1) newUsers.push({ userId: userEvent.userId, nickname: userEvent.nickname, conn: 'CONNECT', profileImageUrl: userEvent.profileImageUrl });
                    else newUsers[userIndex].conn = 'CONNECT';
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

    const value = {
        joinedRooms,
        activeRoomId,
        setActiveRoomId,
        messagesByRoom,
        usersByRoom,
        stompClientsRef,
        initializeChat,
    };

    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export { ChatContext, ChatProvider };