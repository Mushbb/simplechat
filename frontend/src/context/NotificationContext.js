import React, { createContext, useState, useEffect, useRef, useContext, useCallback } from 'react';
import { toast } from 'react-toastify';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axiosInstance from '../api/axiosInstance';
import NotificationToast from '../components/NotificationToast';
import { AuthContext } from './AuthContext';
import { ChatContext } from './ChatContext';
import { FriendContext } from './FriendContext';

const SERVER_URL = axiosInstance.getUri();
const NotificationContext = createContext();

function NotificationProvider({ children }) {
    const { user } = useContext(AuthContext);
    const { joinRoomAndConnect } = useContext(ChatContext);
    const { setFriends } = useContext(FriendContext);
    const [notifications, setNotifications] = useState([]);
    const stompClientRef = useRef(null);

    const rejectNotification = useCallback(async (notificationId) => {
        try {
            await axiosInstance.delete(`/api/notifications/${notificationId}/reject`);
            toast.info('요청을 거절했습니다.');
            setNotifications(prev => prev.filter(n => n.notificationId !== notificationId));
        } catch (error) {
            toast.error('요청 거절에 실패했습니다.');
        }
    }, []);

    const acceptNotification = useCallback(async (notification) => {
        try {
            await axiosInstance.put(`/api/notifications/${notification.notificationId}/accept`);
            toast.info('요청을 수락했습니다.');
            setNotifications(prev => prev.filter(n => n.notificationId !== notification.notificationId));

            if (notification.type === 'ROOM_INVITATION') {
                if (joinRoomAndConnect) {
                    const metadata = JSON.parse(notification.metadata);
                    const newRoom = { id: metadata.roomId, name: metadata.roomName };
                    joinRoomAndConnect(newRoom);
                }
                const metadata = JSON.parse(notification.metadata);
                return metadata.roomId;
            }
            return null;
        } catch (error) {
            toast.error('요청 수락에 실패했습니다.');
            console.error(error);
        }
    }, [joinRoomAndConnect]);

    useEffect(() => {
        if (user) {
            axiosInstance.get('/api/notifications')
                .then(response => setNotifications(response.data))
                .catch(error => console.error('Failed to fetch notifications', error));

            const socket = new SockJS(`${SERVER_URL}/ws`);
            const stompClient = new Client({
                webSocketFactory: () => socket,
                onConnect: () => {
                    stompClient.subscribe(`/user/queue/notifications`, (message) => {
                        const data = JSON.parse(message.body);

                        if (data.type === 'FRIEND_ADDED' || data.type === 'FRIEND_ACCEPTED') {
                            setFriends(prevFriends => 
                                prevFriends.some(f => f.userId === data.friend.userId) ? prevFriends : [...prevFriends, data.friend]
                            );
                            toast.info(`${data.friend.nickname}님과 친구가 되었습니다.`);
                        } else if (data.notificationId) {
                            const notification = data;
                            if (notification.type === 'PRESENCE_UPDATE') {
                                const payload = JSON.parse(notification.metadata);
                                const { userId, isOnline } = payload;
                                setFriends(prevFriends =>
                                    prevFriends.map(friend =>
                                        friend.userId === userId ? { ...friend, conn: isOnline ? 'CONNECT' : 'DISCONNECT' } : friend
                                    )
                                );
                            } else {
                                setNotifications(prev =>
                                    prev.find(n => n.notificationId === notification.notificationId) ? prev : [notification, ...prev]
                                );
                                toast(({ closeToast }) => (
                                    <NotificationToast
                                        notification={notification}
                                        onAccept={acceptNotification}
                                        onReject={rejectNotification}
                                        closeToast={closeToast}
                                    />
                                ), { toastId: notification.notificationId });
                            }
                        } else {
                            console.error("Unknown notification format received:", data);
                        }
                    });
                },
            });
            stompClient.activate();
            stompClientRef.current = stompClient;

            return () => { if (stompClient?.active) stompClient.deactivate(); };
        } else {
            setNotifications([]);
        }
    }, [user, setFriends, acceptNotification, rejectNotification]);

    const value = {
        notifications,
        acceptNotification,
        rejectNotification,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export { NotificationContext, NotificationProvider };
