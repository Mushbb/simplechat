import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { AuthContext } from './AuthContext';
import { RoomContext } from './RoomContext';
import { WebSocketContext } from './WebSocketContext';
import axiosInstance from '../api/axiosInstance';
import { toast } from 'react-toastify';

/**
 * @file 채팅 관련 상태(메시지, 사용자 목록) 및 로직을 관리하고 제공하는 컨텍스트입니다.
 * WebSocket을 통해 수신되는 실시간 데이터를 처리하고, 채팅방 초기화 및 메시지 로딩 기능을 담당합니다.
 */

/**
 * @typedef {object} Message
 * @property {number} messageId
 * @property {number} authorId
 * @property {string} authorName
 * @property {string} authorProfileImageUrl
 * @property {string} content
 * @property {string} messageType - "TEXT", "IMAGE", "DELETE", "UPDATE" 등
 * @property {string} createdAt
 * @property {object} [linkPreview] - 링크 미리보기 정보 (선택적)
 */

/**
 * @typedef {object} ChatUser
 * @property {number} userId
 * @property {string} nickname
 * @property {'CONNECT'|'DISCONNECT'} conn
 * @property {string} profileImageUrl
 * @property {'ADMIN'|'MEMBER'} role
 */

/**
 * @typedef {object} ChatContextType
 * @property {Object<number, Message[]>} messagesByRoom - 방 ID를 키로 하는 메시지 목록 객체.
 * @property {Object<number, ChatUser[]>} usersByRoom - 방 ID를 키로 하는 사용자 목록 객체.
 * @property {Object<number, boolean>} isRoomLoading - 방 ID를 키로 하는 로딩 상태 객체.
 * @property {Object<number, boolean>} hasMoreMessagesByRoom - 방 ID를 키로 하는 추가 메시지 존재 여부 객체.
 * @property {(roomId: number) => void} loadMoreMessages - 이전 메시지를 더 불러오는 함수.
 * @property {React.MutableRefObject<Map<number, import('@stomp/stompjs').Client>>} stompClientsRef - STOMP 클라이언트 Ref.
 * @property {(roomId: number, messageId: number) => Promise<void>} handleDeleteMessage - 메시지 삭제 처리 함수.
 */

/**
 * 채팅 컨텍스트 객체입니다.
 * @type {React.Context<ChatContextType>}
 */
const ChatContext = createContext();

/**
 * 채팅 관련 상태와 기능을 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} ChatContext.Provider
 */
function ChatProvider({ children }) {
    const { user, loading } = useContext(AuthContext);
    const { joinedRooms, activeRoomId, setUnreadRooms, myRole, setMyRole } = useContext(RoomContext);
    const { connectToRoom, stompClientsRef } = useContext(WebSocketContext);

    /** @type {[Object<number, Message[]>, Function]} */
    const [messagesByRoom, setMessagesByRoom] = useState({});
    /** @type {[Object<number, ChatUser[]>, Function]} */
    const [usersByRoom, setUsersByRoom] = useState({});
    /** @type {[Object<number, boolean>, Function]} */
    const [isRoomLoading, setIsRoomLoading] = useState({});
    /** @type {[Object<number, boolean>, Function]} */
    const [hasMoreMessagesByRoom, setHasMoreMessagesByRoom] = useState({});

    /**
     * WebSocket을 통해 추가 메시지(이전 대화) 목록을 수신했을 때 호출되는 콜백 함수.
     * @param {object} response - { roomId, messages } 형태의 응답 객체.
     */
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

    /**
     * WebSocket을 통해 새로운 메시지(공개 메시지)를 수신했을 때 호출되는 콜백 함수.
     * 메시지 타입에 따라(DELETE, UPDATE 등) 상태를 업데이트합니다.
     * @param {number} roomId - 메시지가 수신된 방의 ID.
     * @param {import('@stomp/stompjs').Message} payload - STOMP 메시지 페이로드.
     */
    const onMessageReceived = useCallback((roomId, payload) => {
        const message = JSON.parse(payload.body);
        
        if (message.messageType === 'DELETE') {
            setMessagesByRoom(prev => {
                const currentMessages = prev[roomId] || [];
                const updatedMessages = currentMessages.filter(m => m.messageId !== message.messageId);
                return { ...prev, [roomId]: updatedMessages };
            });
            return;
        }

        if (message.messageType === 'UPDATE') {
            setMessagesByRoom(prev => {
                const currentMessages = prev[roomId] || [];
                const updatedMessages = currentMessages.map(m => 
                    m.messageId === message.messageId ? { ...m, content: message.content } : m
                );
                return { ...prev, [roomId]: updatedMessages };
            });
            return;
        }

        if (roomId !== activeRoomId) {
            setUnreadRooms(prev => new Set(prev).add(roomId));
        }
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

    /**
     * WebSocket을 통해 사용자 관련 이벤트(입장, 퇴장, 닉네임 변경 등)를 수신했을 때 호출되는 콜백 함수.
     * @param {number} roomId - 이벤트가 발생한 방의 ID.
     * @param {import('@stomp/stompjs').Message} payload - STOMP 메시지 페이로드.
     */
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

    /**
     * WebSocket을 통해 링크 미리보기 정보를 수신했을 때 호출되는 콜백 함수.
     * @param {number} roomId - 미리보기가 수신된 방의 ID.
     * @param {import('@stomp/stompjs').Message} payload - STOMP 메시지 페이로드.
     */
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

    /**
     * 참여한 모든 채팅방에 대한 WebSocket 연결을 설정하고 초기 데이터를 로드하는 Effect.
     * 사용자가 로그인하거나 참여한 방 목록이 변경될 때 실행됩니다.
     */
    useEffect(() => {
        if (user && joinedRooms.length > 0) {
            joinedRooms.forEach(room => {
                if (!stompClientsRef.current.has(room.id)) {
                    setIsRoomLoading(prev => ({ ...prev, [room.id]: true }));
                    connectToRoom(room.id, onMessageReceived, onUserInfoReceived, onPreviewReceived, onMoreMessagesReceived)
                        .then(() => {
                            axiosInstance.get(`/room/${room.id}/init?lines=20`).then(response => {
                                const data = response.data;
                                const users = data.users || [];
                                setUsersByRoom(prev => ({ ...prev, [room.id]: users }));
                                
                                const me = users.find(u => u.userId === user.userId);
                                if (me) {
                                    setMyRole(prev => ({ ...prev, [room.id]: me.role }));
                                }

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
    }, [user, joinedRooms, connectToRoom, onMessageReceived, onUserInfoReceived, onPreviewReceived, onMoreMessagesReceived, stompClientsRef, setMyRole]);

    /**
     * 특정 채팅방의 이전 메시지를 더 불러옵니다.
     * @param {number} roomId - 메시지를 불러올 방의 ID.
     */
    const loadMoreMessages = (roomId) => {
        const client = stompClientsRef.current.get(roomId);
        const currentMessages = messagesByRoom[roomId] || [];
        if (!client?.connected || currentMessages.length === 0) return;

        const oldestMessage = currentMessages[0];
        const requestDto = { roomId: roomId, beginId: oldestMessage.messageId, rowCount: 20 };
        client.publish({ destination: '/app/chat.getMessageList', body: JSON.stringify(requestDto) });
    };

    /**
     * 특정 메시지를 삭제하는 함수. (향후 구현 예정)
     * @param {number} roomId - 메시지가 속한 방의 ID.
     * @param {number} messageId - 삭제할 메시지의 ID.
     */
    const handleDeleteMessage = async (roomId, messageId) => {
        console.log(`(구현 예정) ${roomId}번 방의 ${messageId} 메시지를 삭제합니다.`);
    };

    const value = {
        messagesByRoom,
        usersByRoom,
        isRoomLoading,
        hasMoreMessagesByRoom,
        loadMoreMessages,
        stompClientsRef,
        handleDeleteMessage,
    };

    return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export { ChatContext, ChatProvider };