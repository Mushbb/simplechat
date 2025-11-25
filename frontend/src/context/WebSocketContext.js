import React, { createContext, useContext, useRef, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthContext } from './AuthContext';
import { RoomContext } from './RoomContext';
import axiosInstance from '../api/axiosInstance';

/**
 * @file WebSocket 및 STOMP 연결을 전역적으로 관리하고 제공하는 컨텍스트입니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * @typedef {object} WebSocketContextType
 * @property {React.MutableRefObject<Map<number, Client>>} stompClientsRef - 활성화된 STOMP 클라이언트 인스턴스들을 저장하는 Ref 객체. (Key: roomId, Value: STOMP Client)
 * @property {(roomId: number, onMessage: Function, onUserInfo: Function, onPreview: Function, onMoreMessages: Function) => Promise<Client>} connectToRoom - 특정 채팅방에 대한 WebSocket 연결을 설정하고 활성화하는 함수.
 * @property {() => Promise<void>} initializeConnections - 모든 활성 WebSocket 연결을 초기화(연결 해제 및 정리)하는 함수.
 */

/**
 * WebSocket 컨텍스트 객체입니다.
 * @type {React.Context<WebSocketContextType>}
 */
const WebSocketContext = createContext();

/**
 * WebSocket 연결 상태를 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} WebSocketContext.Provider
 */
function WebSocketProvider({ children }) {
    const { user, loading, forceLogout } = useContext(AuthContext);
    const { setJoinedRooms } = useContext(RoomContext);
    /**
     * 채팅방별 STOMP 클라이언트 인스턴스를 관리하는 Ref.
     * key는 roomId, value는 STOMP Client 인스턴스입니다.
     * @type {React.MutableRefObject<Map<number, Client>>}
     */
    const stompClientsRef = useRef(new Map());

    /**
     * 특정 채팅방에 대한 WebSocket 연결을 설정하고 활성화합니다.
     * @param {number} roomId - 연결할 채팅방의 ID.
     * @param {Function} onMessage - 공개 메시지 수신 시 호출될 콜백 함수.
     * @param {Function} onUserInfo - 사용자 입장/퇴장/상태 변경 등 사용자 정보 수신 시 호출될 콜백 함수.
     * @param {Function} onPreview - 링크 미리보기 정보 수신 시 호출될 콜백 함수.
     * @param {Function} onMoreMessages - 추가 메시지 목록(이전 대화) 수신 시 호출될 콜백 함수.
     * @returns {Promise<Client>} 연결 성공 시 STOMP 클라이언트 인스턴스를 resolve하는 프로미스.
     */
    const connectToRoom = (roomId, onMessage, onUserInfo, onPreview, onMoreMessages) => {
        return new Promise((resolve, reject) => {
            if (!user || stompClientsRef.current.has(roomId)) {
                resolve();
                return;
            }

            const client = new Client({
                webSocketFactory: () => new SockJS(`${SERVER_URL}/ws`),
                connectHeaders: { user_id: String(user.userId), room_id: String(roomId) },
                reconnectDelay: 0,
                onConnect: () => {
                    console.log(`Room #${roomId}: WebSocket connected`);
                    client.subscribe('/user/topic/queue/reply', (payload) => onMoreMessages(JSON.parse(payload.body)));
                    client.subscribe(`/topic/${roomId}/public`, (payload) => onMessage(roomId, payload));
                    client.subscribe(`/topic/${roomId}/users`, (payload) => onUserInfo(roomId, payload));
                    client.subscribe(`/topic/${roomId}/previews`, (payload) => onPreview(roomId, payload));
                    resolve(client);
                },
                onDisconnect: () => {
                    console.error(`Room #${roomId}: WebSocket disconnected`);
                    forceLogout();
                },
                onStompError: (frame) => {
                    console.error('STOMP Error:', frame.headers['message'], frame.body);
                    forceLogout();
                    reject(new Error(frame.headers['message']));
                },
                onWebSocketError: (error) => {
                    console.error('WebSocket Error:', error);
                    forceLogout();
                },
            });

            client.activate();
            stompClientsRef.current.set(roomId, client);
        });
    };

    /**
     * 모든 활성화된 WebSocket 연결을 초기화(연결 해제 및 정리)합니다.
     * 주로 사용자가 로그인/로그아웃할 때 호출됩니다.
     */
    const initializeConnections = async () => {
        if (!user) return;
        try {
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
        } catch (error) {
            console.error("Failed to clear connections:", error);
        }
    };

    /**
     * 사용자 인증 상태 변경 시 WebSocket 연결을 관리하는 Effect.
     * 사용자가 로그인하면 기존 연결을 초기화하고, 로그아웃하면 모든 연결을 해제합니다.
     */
    useEffect(() => {
        if (!loading && user) {
            initializeConnections();
        } else if (!loading && !user) {
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
        }

        return () => {
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
        };
    }, [user, loading]);

    const value = {
        stompClientsRef,
        connectToRoom,
        initializeConnections,
    };

    return (
        <WebSocketContext.Provider value={value}>
            {children}
        </WebSocketContext.Provider>
    );
}

export { WebSocketContext, WebSocketProvider };
