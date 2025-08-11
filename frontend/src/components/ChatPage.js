import React, { useState, useEffect, useContext, useRef, useLayoutEffect } from 'react';
import { useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import ChatMessage from './ChatMessage';
import ProfileModal from './ProfileModal';
import axiosInstance from '../api/axiosInstance';

function ChatPage() {
    const { roomId } = useParams();
    const { user } = useContext(AuthContext);
    const [messages, setMessages] = useState([]);
    const [users, setUsers] = useState([]);
    const [newMessage, setNewMessage] = useState('');

    const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });

    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);

    const stompClientRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);
    const sentinelRef = useRef(null);
    const scrollActionRef = useRef('initial');

    useEffect(() => {
        if (!user || !roomId) return;

        scrollActionRef.current = 'initial';

        const client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
            connectHeaders: { user_id: String(user.userId), room_id: String(roomId) },
            onConnect: () => {
                stompClientRef.current = client;
                client.subscribe(`/topic/${roomId}/users`, onUserInfoReceived);
                client.subscribe(`/topic/${roomId}/public`, onMessageReceived);
                client.subscribe('/user/topic/queue/reply', onOlderMessagesReceived);
                client.subscribe(`/topic/${roomId}/previews`, onPreviewReceived);
            },
        });

        const initChat = async () => {
            try {
                const response = await axiosInstance.get(`/room/${roomId}/init?lines=20`);
                const data = response.data;
                setUsers(data.users || []);
                setMessages([...(data.messages || [])].reverse());
            } catch (error) { console.error('Initialization failed:', error); }
        };

        initChat();
        client.activate();

        return () => { if(stompClientRef.current) stompClientRef.current.deactivate(); };
    }, [user, roomId]);


    const onMessageReceived = (payload) => {
        scrollActionRef.current = 'new'; // ✅ 새 메시지 수신 시 'new'로 기록
        const receivedMessage = JSON.parse(payload.body);
        prevScrollHeightRef.current = null; // 새 메시지 수신 시 자동 스크롤을 위해 null로 설정
        setMessages(prev => [...prev, receivedMessage]);
    };

    const onUserInfoReceived = (payload) => {
        const userEvent = JSON.parse(payload.body);
        setUsers(currentUsers => {
            const userIndex = currentUsers.findIndex(u => u.userId === userEvent.userId);
            let newUsers = [...currentUsers];
            switch (userEvent.eventType) {
                case 'ENTER':
                    if (userIndex === -1) newUsers.push({ userId: userEvent.userId, nickname: userEvent.nickname, role: userEvent.role, conn: 'CONNECT' });
                    else newUsers[userIndex].conn = 'CONNECT';
                    break;
                case 'EXIT':
                    if (userIndex !== -1) newUsers[userIndex].conn = 'DISCONNECT';
                    break;
                default: break;
            }
            return newUsers;
        });
    };

    const onOlderMessagesReceived = (payload) => {
        const olderMessages = JSON.parse(payload.body).msgList;
        if (olderMessages && olderMessages.length > 0) {
            // ✅ 1. 새 메시지가 추가되기 직전의 현재 스크롤 높이를 기록합니다.
            if (scrollContainerRef.current) {
                prevScrollHeightRef.current = scrollContainerRef.current.scrollHeight;
            }
            scrollActionRef.current = 'old'; // ✅ 이전 메시지 수신 시 'old'로 기록
            setMessages(prev => [...olderMessages.reverse(), ...prev]);
        } else {
            setHasMore(false);
        }
        setIsLoading(false);
    };

    const onPreviewReceived = (payload) => {
        const preview = JSON.parse(payload.body);
        setMessages(prev => prev.map(msg =>
            msg.messageId === preview.messageId ? { ...msg, linkPreview: preview } : msg
        ));
    };

    // --- ✅ 여기가 수정된 핵심 스크롤 로직입니다 ---
    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (!container) return;

        if (scrollActionRef.current === 'old') {
            // '이전 메시지 로딩' 액션일 때만 위치 보정
            const prevHeight = prevScrollHeightRef.current;
            if (typeof prevHeight === 'number') {
                container.scrollTop = container.scrollHeight - prevHeight;
            }
        } else {
            // '새 메시지' 또는 '초기 로딩' 액션일 때는 맨 아래로
            container.scrollTop = container.scrollHeight;
        }

        // ❗️주의: 여기서 Ref를 null로 리셋하지 않아, 다음 액션 전까지 상태가 유지됩니다.
        // scrollActionRef.current = null;  <- 이 줄을 제거한 것이 핵심!

    }, [messages]);


    useEffect(() => {
        if (!hasMore || !scrollContainerRef.current) return;
        const observer = new IntersectionObserver(([entry]) => {
            if (entry.isIntersecting && !isLoading) {
                setIsLoading(true);
                const firstMessageId = messages[0]?.messageId;
                if (firstMessageId && stompClientRef.current?.connected) {
                    stompClientRef.current.publish({
                        destination: '/app/chat.getMessageList',
                        body: JSON.stringify({ roomId, beginId: firstMessageId, rowCount: 20 }),
                    });
                } else {
                    setIsLoading(false);
                }
            }
        }, { root: scrollContainerRef.current, threshold: 0.1 });

        const currentSentinel = sentinelRef.current;
        if (currentSentinel) observer.observe(currentSentinel);

        return () => { if (currentSentinel) observer.unobserve(currentSentinel); };
    }, [isLoading, hasMore, messages, roomId]);


    // --- 사용자 클릭 및 모달 위치 계산 로직 수정 ---
    const handleUserClick = async (clickedUserId, event) => {
        // 클릭된 li 요소의 화면상 위치 정보를 가져옵니다.
        const liRect = event.currentTarget.getBoundingClientRect();
        // 기준점이 될 컨테이너의 화면상 위치 정보를 가져옵니다.
        const containerRect = event.currentTarget.closest('[data-id="chat-main-flex-container"]').getBoundingClientRect();

        // 컨테이너를 기준으로 모달이 표시될 상대 위치를 계산합니다.
        const position = {
            // li의 top에서 컨테이너의 top을 빼서 상대적인 top 위치를 구합니다.
            top: liRect.top - containerRect.top,
            // li의 왼쪽에 컨테이너 왼쪽 위치를 빼고, li의 너비만큼 더해 오른쪽에 표시합니다.
            left: liRect.left - containerRect.left + liRect.width + 10,
        };
        setModalPosition(position);

        try {
            const response = await axiosInstance.get(`/user/${clickedUserId}/profile`);
            setSelectedProfile(response.data);
            setIsProfileModalOpen(true);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            alert('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };

    const closeProfileModal = () => {
        setIsProfileModalOpen(false);
        setSelectedProfile(null);
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (newMessage.trim() && stompClientRef.current?.connected) {
            const chatMessage = { roomId, authorId: user.userId, content: newMessage, messageType: 'TEXT' };
            stompClientRef.current.publish({ destination: '/app/chat.sendMessage', body: JSON.stringify(chatMessage) });
            setNewMessage('');
        }
    };

    if (!user) { return <h2>로그인이 필요합니다.</h2>; }

    return (
        // 최상위 div에는 별도 스타일이 없습니다.
        <div>
            <h2>채팅방 #{roomId}</h2>
            <div
                data-id="chat-main-flex-container"
                style={{
                    position: 'relative',
                    display: 'flex',
                    height: 'calc(100vh - 200px)'
                }}
            >
                {isProfileModalOpen && (
                    <ProfileModal
                        profile={selectedProfile}
                        onClose={closeProfileModal}
                        position={modalPosition}
                    />
                )}
                {/* 👇 사용자 목록 패널이 모달의 기준점이 됩니다. */}
                <div
                    data-id="user-list-panel"
                    style={{
                        position: 'relative', // ✅ 이 스타일이 핵심입니다.
                        width: '200px',
                        borderRight: '1px solid #ccc',
                        padding: '10px',
                        overflowY: 'auto'
                    }}
                >
                    <h3>참가자</h3>
                    <ul style={{ listStyle: 'none', padding: 0 }}>
                        {users.map(u => (
                            <li
                                key={u.userId}
                                style={{
                                    color: u.conn === 'DISCONNECT' ? 'gray' : 'black',
                                    padding: '8px 5px',
                                    cursor: 'pointer',
                                    borderRadius: '4px',
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f0f0f0'}
                                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                                onClick={(event) => handleUserClick(u.userId, event)}
                            >
                                {u.nickname}
                            </li>
                        ))}
                    </ul>
                    {/* 모달을 기준점 내부에 렌더링합니다. */}
                    {isProfileModalOpen && (
                        <ProfileModal
                            profile={selectedProfile}
                            onClose={closeProfileModal}
                            position={modalPosition}
                        />
                    )}
                </div>

                {/* 👇 이 채팅 영역은 이제 영향을 받지 않습니다. */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div ref={scrollContainerRef} style={{ flex: 1, overflowY: 'auto', padding: '10px', borderBottom: '1px solid #ccc' }}>
                        {isLoading && <div style={{ textAlign: 'center' }}>이전 대화 불러오는 중...</div>}
                        {hasMore && <div ref={sentinelRef} style={{ height: '1px' }} />}
                        {messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)}
                    </div>
                    <form onSubmit={handleSendMessage} style={{ padding: '10px', display: 'flex' }}>
                        <input type="text" style={{ flex: 1, marginRight: '10px' }} value={newMessage} onChange={(e) => setNewMessage(e.target.value)} placeholder="메시지 입력..." />
                        <button type="submit">전송</button>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default ChatPage;