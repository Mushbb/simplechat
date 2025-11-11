import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { AuthContext } from './AuthContext';
import { RoomContext } from './RoomContext';
import { WebSocketContext } from './WebSocketContext'; // Import WebSocketContext
import axiosInstance from '../api/axiosInstance';

const ChatContext = createContext();

function ChatProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { joinedRooms, activeRoomId, setUnreadRooms } = useContext(RoomContext);
    const { connectToRoom, stompClientsRef } = useContext(WebSocketContext); // Get from WebSocketContext

    const [messagesByRoom, setMessagesByRoom] = useState({});
    const [usersByRoom, setUsersByRoom] = useState({});
    const [isRoomLoading, setIsRoomLoading] = useState({});
    const [hasMoreMessagesByRoom, setHasMoreMessagesByRoom] = useState({});

    const onMoreMessagesReceived = useCallback((response) => {
        const { roomId, messages } = response;
        if (!roomId || !messages) return;

        if (messages.length === 0) {
            setHasMoreMessagesByRoom(prev => ({ ...prev, [roomId]: false }));
            return;
        }

        setMessagesByRoom(prev => {
            const currentMessages = prev[roomId] || [];
            const existingMessageIds = new Set(currentMessages.map(msg => msg.messageId));
            const newMessages = messages.filter(msg => !existingMessageIds.has(msg.messageId));
            if (newMessages.length === 0) return prev;
            return { ...prev, [roomId]: [...newMessages, ...currentMessages] };
        });
    }, []);

    const onMessageReceived = useCallback((roomId, payload) => {
        if (roomId !== activeRoomId) {
            setUnreadRooms(prev => new Set(prev).add(roomId));
        }
        const message = JSON.parse(payload.body);
        setMessagesByRoom(prev => ({ ...prev, [roomId]: [...(prev[roomId] || []), message] }));
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
    }, [activeRoomId, setUnreadRooms]);

    const onUserInfoReceived = useCallback((roomId, payload) => {
        const userEvent = JSON.parse(payload.body);
        setUsersByRoom(prev => {
            const currentUsers = prev[roomId] || [];
            let newUsers = [...currentUsers];
            const userIndex = currentUsers.findIndex(u => u.userId === userEvent.userId);
            switch (userEvent.eventType) {
                case 'ENTER':
                case 'ROOM_IN':
                    if (userIndex === -1) newUsers.push({ userId: userEvent.userId, nickname: userEvent.nickname, conn: 'CONNECT', profileImageUrl: userEvent.profileImageUrl, role: userEvent.role });
                    else newUsers[userIndex].conn = 'CONNECT';
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
                default: break;
            }
            return { ...prev, [roomId]: newUsers };
        });
    }, []);

    const onPreviewReceived = useCallback((roomId, payload) => {
        const preview = JSON.parse(payload.body);
        setMessagesByRoom(prev => {
            const currentMessages = prev[roomId] || [];
            const updatedMessages = currentMessages.map(msg =>
                msg.messageId === preview.messageId ? { ...msg, linkPreview: preview } : msg
            );
            return { ...prev, [roomId]: updatedMessages };
        });
    }, []);

    useEffect(() => {
        if (user && joinedRooms.length > 0) {
            joinedRooms.forEach(room => {
                if (!stompClientsRef.current.has(room.id)) {
                    setIsRoomLoading(prev => ({ ...prev, [room.id]: true }));
                    connectToRoom(room.id, onMessageReceived, onUserInfoReceived, onPreviewReceived, onMoreMessagesReceived)
                        .then(() => {
                            axiosInstance.get(`/room/${room.id}/init?lines=20`).then(response => {
                                const data = response.data;
                                setUsersByRoom(prev => ({ ...prev, [room.id]: data.users || [] }));
                                setMessagesByRoom(prev => ({ ...prev, [room.id]: [...(data.messages || [])].reverse() }));
                                const initialMessages = response.data.messages || [];
                                setHasMoreMessagesByRoom(prev => ({ ...prev, [room.id]: initialMessages.length >= 20 }));
                            }).catch(error => {
                                console.error(`#${room.id} 방 초기화 데이터 로딩 실패:`, error);
                            }).finally(() => {
                                setIsRoomLoading(prev => ({ ...prev, [room.id]: false }));
                            });
                        });
                }
            });
        }
    }, [user, joinedRooms, connectToRoom, onMessageReceived, onUserInfoReceived, onPreviewReceived, onMoreMessagesReceived, stompClientsRef]);

    const loadMoreMessages = (roomId) => {
        const client = stompClientsRef.current.get(roomId);
        const currentMessages = messagesByRoom[roomId] || [];
        if (!client?.connected || currentMessages.length === 0) return;

        const oldestMessage = currentMessages[0];
        const requestDto = { roomId: roomId, beginId: oldestMessage.messageId, rowCount: 20 };
        client.publish({ destination: '/app/chat.getMessageList', body: JSON.stringify(requestDto) });
    };

    const value = {
        messagesByRoom,
        usersByRoom,
        isRoomLoading,
        hasMoreMessagesByRoom,
        loadMoreMessages,
        stompClientsRef,
    };

    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export { ChatContext, ChatProvider };