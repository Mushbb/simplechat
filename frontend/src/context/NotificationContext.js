import React, { createContext, useState, useEffect, useRef, useContext, useCallback } from 'react';
import { toast } from 'react-toastify';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axiosInstance from '../api/axiosInstance';
import NotificationToast from '../components/NotificationToast';
import { AuthContext } from './AuthContext';
import { FriendContext } from './FriendContext';
import { RoomContext } from './RoomContext';

/**
 * @file 실시간 알림을 관리하고 관련 UI 상호작용을 처리하는 컨텍스트입니다.
 * WebSocket을 통해 알림을 수신하고, 상태를 업데이트하며, 알림에 대한 사용자 액션(수락, 거절 등)을 처리합니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * @typedef {object} Notification
 * @property {number} notificationId
 * @property {string} type - "FRIEND_REQUEST", "ROOM_INVITATION", "MENTION" 등
 * @property {string} content
 * @property {string} metadata
 * @property {boolean} isRead
 * @property {string} createdAt
 */

/**
 * @typedef {object} NotificationContextType
 * @property {Notification[]} notifications - 현재 알림 목록.
 * @property {number} unreadCount - 읽지 않은 알림의 개수.
 * @property {(notification: Notification) => Promise<number|null>} acceptNotification - 알림을 수락하는 함수. 방 초대 수락 시 roomId 반환.
 * @property {(notificationId: number) => Promise<void>} rejectNotification - 알림을 거절하는 함수.
 * @property {(notificationIds: number[]) => Promise<void>} markNotificationsAsRead - 여러 알림을 읽음으로 처리하는 함수.
 */

/**
 * 알림 컨텍스트 객체입니다.
 * @type {React.Context<NotificationContextType>}
 */
const NotificationContext = createContext();

/**
 * 알림 관련 상태와 기능을 제공하는 React 컴포넌트입니다.
 * @param {object} props
 * @param {React.ReactNode} props.children - 이 Provider가 감쌀 자식 컴포넌트들.
 * @returns {JSX.Element} NotificationContext.Provider
 */
function NotificationProvider({ children }) {
    const { user } = useContext(AuthContext);
    const { joinRoomAndConnect } = useContext(RoomContext);
    const { setFriends } = useContext(FriendContext);

    /** @type {[Notification[], React.Dispatch<React.SetStateAction<Notification[]>>]} */
    const [notifications, setNotifications] = useState([]);
    /** @type {[number, React.Dispatch<React.SetStateAction<number>>]} */
    const [unreadCount, setUnreadCount] = useState(0);
    const stompClientRef = useRef(null);

    /**
     * 특정 알림을 거절하고 목록에서 제거합니다.
     * @param {number} notificationId - 거절할 알림의 ID.
     * @returns {Promise<void>}
     */
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

    /**
     * 특정 알림을 수락하고, 알림 유형에 따라 후속 조치를 실행합니다.
     * @param {Notification} notification - 수락할 알림 객체.
     * @returns {Promise<number|null>} 방 초대 알림인 경우, 참여하게 될 방의 ID를 반환합니다. 그 외에는 null을 반환합니다.
     */
    const acceptNotification = useCallback(async (notification) => {
        try {
            await axiosInstance.put(`/api/notifications/${notification.notificationId}/accept`);
            toast.info('요청을 수락했습니다.');
            setNotifications(prev => {
                const newNotifications = prev.filter(n => n.notificationId !== notification.notificationId);
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

    /**
     * 알림 ID 목록을 받아 해당 알림들을 읽음 상태로 변경합니다.
     * @param {number[]} notificationIds - 읽음으로 표시할 알림들의 ID 배열.
     * @returns {Promise<void>}
     */
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

    /**
     * 사용자 로그인 상태에 따라 알림 관련 기능을 초기화하고 WebSocket 연결을 관리하는 Effect.
     * 로그인 시: 기존 알림을 불러오고, 알림용 WebSocket에 연결하여 실시간 수신을 시작합니다.
     * 로그아웃 시: 상태를 초기화하고 연결을 해제합니다.
     */
    useEffect(() => {
        if (user) {
            axiosInstance.get('/api/notifications')
                .then(response => {
                    setNotifications(response.data);
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
                                    console.log(`Room ${data.relatedEntityId}으로 이동`);
                                }
                            });
                            setUnreadCount(prevCount => prevCount + 1);

                        } else if (data.notificationId) {
                            const notification = data;
                            setNotifications(prev => {
                                const newNotifications = prev.find(n => n.notificationId === notification.notificationId)
                                    ? prev
                                    : [notification, ...prev];
                                return newNotifications;
                            });
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
            setUnreadCount(0);
        }
    }, [user, setFriends, acceptNotification, rejectNotification, markNotificationsAsRead]);

    const value = {
        notifications,
        unreadCount,
        acceptNotification,
        rejectNotification,
        markNotificationsAsRead,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export { NotificationContext, NotificationProvider };
