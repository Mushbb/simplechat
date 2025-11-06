import React, { createContext, useContext, useRef, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthContext } from './AuthContext';
import { RoomContext } from './RoomContext';
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = axiosInstance.getUri();
const WebSocketContext = createContext();

function WebSocketProvider({ children }) {
    const { user, loading, forceLogout } = useContext(AuthContext);
    const { setJoinedRooms } = useContext(RoomContext);
    const stompClientsRef = useRef(new Map());

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

    const initializeConnections = async () => {
        if (!user) return;
        try {
            stompClientsRef.current.forEach(client => client.deactivate());
            stompClientsRef.current.clear();
            
            const response = await axiosInstance.get(`${SERVER_URL}/api/my-rooms`);
            const myRooms = response.data;
            setJoinedRooms(myRooms);
            // Note: Connection to rooms will be initiated by ChatContext
        } catch (error) {
            console.error("Failed to initialize connections:", error);
        }
    };

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
