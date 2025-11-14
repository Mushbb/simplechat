import React, { createContext, useState, useEffect, useRef, useContext, useCallback } from 'react';
import { toast } from 'react-toastify';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axiosInstance from '../api/axiosInstance';
import NotificationToast from '../components/NotificationToast';
import { AuthContext } from './AuthContext';
import { FriendContext } from './FriendContext';
import { RoomContext } from './RoomContext';

const SERVER_URL = axiosInstance.getUri();
const NotificationContext = createContext();

function NotificationProvider({ children }) {
    const { user } = useContext(AuthContext);
    const { joinRoomAndConnect } = useContext(RoomContext);
    const { setFriends } = useContext(FriendContext);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0); // ✨ 신규: 읽지 않은 알림 개수 상태
    const stompClientRef = useRef(null);

    const rejectNotification = useCallback(async (notificationId) => {
        try {
            await axiosInstance.delete(`/api/notifications/${notificationId}/reject`);
            toast.info('요청을 거절했습니다.');
            setNotifications(prev => {
                const newNotifications = prev.filter(n => n.notificationId !== notificationId);
                // 거절된 알림이 읽지 않은 상태였다면 unreadCount 감소
                if (prev.find(n => n.notificationId === notificationId && !n.isRead)) {
                    setUnreadCount(prevCount => Math.max(0, prevCount - 1));
                }
                return newNotifications;
            });
        } catch (error) {
            toast.error('요청 거절에 실패했습니다.');
        }
    }, []);

    const acceptNotification = useCallback(async (notification) => {
        try {
            await axiosInstance.put(`/api/notifications/${notification.notificationId}/accept`);
            toast.info('요청을 수락했습니다.');
            setNotifications(prev => {
                const newNotifications = prev.filter(n => n.notificationId !== notification.notificationId);
                // 수락된 알림이 읽지 않은 상태였다면 unreadCount 감소
                if (!notification.isRead) {
                    setUnreadCount(prevCount => Math.max(0, prevCount - 1));
                }
                return newNotifications;
            });

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

    // ✨ 신규: 알림을 읽음으로 표시하는 함수
    const markNotificationsAsRead = useCallback(async (notificationIds) => {
        if (!user || notificationIds.length === 0) return;

        try {
            await axiosInstance.put('/api/notifications/mark-as-read', notificationIds);
            setNotifications(prev => {
                let newUnreadCount = unreadCount;
                const updatedNotifications = prev.map(n => {
                    if (notificationIds.includes(n.notificationId) && !n.isRead) {
                        newUnreadCount--;
                        return { ...n, isRead: true };
                    }
                    return n;
                });
                setUnreadCount(Math.max(0, newUnreadCount));
                return updatedNotifications;
            });
        } catch (error) {
            console.error('Failed to mark notifications as read', error);
            toast.error('알림을 읽음으로 표시하는 데 실패했습니다.');
        }
    }, [user, unreadCount]);

    useEffect(() => {
        if (user) {
            axiosInstance.get('/api/notifications')
                .then(response => {
                    setNotifications(response.data);
                    // ✨ 신규: 초기 로드 시 읽지 않은 알림 개수 계산
                    const initialUnread = response.data.filter(n => !n.isRead).length;
                    setUnreadCount(initialUnread);
                })
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
                        
                        } else if (data.type === 'PRESENCE_UPDATE') {
                            const payload = JSON.parse(data.metadata);
                            const { userId, isOnline } = payload;
                            setFriends(prevFriends =>
                                prevFriends.map(friend =>
                                    friend.userId === userId ? { ...friend, conn: isOnline ? 'CONNECT' : 'DISCONNECT' } : friend
                                )
                            );

                        } else if (data.type === 'MENTION') {
                            toast.info(data.content, {
                                onClick: () => {
                                    // 클릭 시 해당 방으로 이동하는 로직 (추후 구현)
                                    console.log(`Room ${data.relatedEntityId}으로 이동`);
                                }
                            });
                            // 멘션 알림도 읽지 않은 알림으로 간주하여 카운트 증가
                            setUnreadCount(prevCount => prevCount + 1);

                        } else if (data.notificationId) {
                            const notification = data;
                            setNotifications(prev => {
                                // 중복 알림 방지 및 새 알림 추가
                                const newNotifications = prev.find(n => n.notificationId === notification.notificationId)
                                    ? prev
                                    : [notification, ...prev];
                                return newNotifications;
                            });
                            // ✨ 신규: isRead가 false인 경우에만 unreadCount 증가
                            if (!notification.isRead) {
                                setUnreadCount(prevCount => prevCount + 1);
                            }
                            toast(({ closeToast }) => (
                                <NotificationToast
                                    notification={notification}
                                    onAccept={acceptNotification}
                                    onReject={rejectNotification}
                                    closeToast={closeToast}
                                />
                            ), { toastId: notification.notificationId });
                        
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
            setUnreadCount(0); // ✨ 신규: 로그아웃 시 unreadCount 초기화
        }
    }, [user, setFriends, acceptNotification, rejectNotification, markNotificationsAsRead]);

    const value = {
        notifications,
        unreadCount, // ✨ 신규: unreadCount 노출
        acceptNotification,
        rejectNotification,
        markNotificationsAsRead, // ✨ 신규: markNotificationsAsRead 노출
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export { NotificationContext, NotificationProvider };
