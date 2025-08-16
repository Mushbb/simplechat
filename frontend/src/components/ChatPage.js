import React, { useState, useEffect, useContext, useRef, useLayoutEffect } from 'react';
import { useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import ChatMessage from './ChatMessage';
import UserProfileModal from './UserProfileModal';
import axiosInstance from '../api/axiosInstance';

// ✅ 1. react-icons 라이브러리에서 원하는 아이콘을 import 합니다.
import { IoSend } from "react-icons/io5";

const SERVER_URL = 'http://localhost:8080';

function ChatPage() {
    const { roomId } = useParams();
    const { user } = useContext(AuthContext);

    const [roomName, setRoomName] = useState('');
    const [messages, setMessages] = useState([]);
    const [users, setUsers] = useState([]);
    const [newMessage, setNewMessage] = useState('');

    const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });

    const [isLoading, setIsLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);

    const [filesToUpload, setFilesToUpload] = useState([]); // 업로드할 파일 목록
    const [isPreviewVisible, setIsPreviewVisible] = useState(false); // 미리보기 UI 표시 여부
    const [myNickname, setMyNickname] = useState('');
    const [isUploading, setIsUploading] = useState(false);

    const stompClientRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);
    const sentinelRef = useRef(null);
    const scrollActionRef = useRef('initial');
    const fileInputRef = useRef(null); // 숨겨진 file input에 접근하기 위한 ref
    const textareaRef = useRef(null); // ✅ textarea를 참조할 ref 생성

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

                setRoomName(data.roomName);
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
                // ✅ 4. NICK_CHANGE 이벤트 처리 로직 추가
                case 'NICK_CHANGE':
                    if (userIndex !== -1) {
                        newUsers[userIndex] = { ...newUsers[userIndex], nickname: userEvent.nickname };
                    }
                    break;
                default:
                    break;
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

    // ✅ 2. users 배열이나 user 객체가 변경될 때 내 닉네임을 찾아서 state에 설정
    useEffect(() => {
        if (user && users.length > 0) {
            const me = users.find(u => u.userId === user.userId);
            if (me) {
                setMyNickname(me.nickname);
            }
        }
    }, [users, user]);

    // ✅ textarea 높이를 자동으로 조절하는 useEffect
    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = '0px'; // 높이를 초기화
            const scrollHeight = textarea.scrollHeight;
            textarea.style.height = `${scrollHeight}px`; // 실제 컨텐츠 높이만큼 설정
        }
    }, [newMessage]); // newMessage가 바뀔 때마다 실행

    // ✅ 3. 닉네임 변경 요청을 서버에 보내는 함수
    const handleNicknameUpdate = () => {
        // 현재 사용자의 원래 닉네임 찾기
        const me = users.find(u => u.userId === user.userId);
        // 닉네임이 변경되지 않았거나, 비어있으면 요청하지 않음
        if (!me || me.nickname === myNickname || myNickname.trim() === '') {
            setMyNickname(me ? me.nickname : ''); // 원래 닉네임으로 되돌림
            return;
        }

        // 서버에 닉네임 변경 메시지 전송
        if (stompClientRef.current?.connected) {
            const nickChangeMessage = {
                roomId,
                userId: user.userId,
                newNickname: myNickname.trim(),
            };
            stompClientRef.current.publish({
                destination: '/app/chat.changeNick',
                body: JSON.stringify(nickChangeMessage),
            });
        }
    };

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

    // ✅ 키보드 이벤트를 처리하는 함수
    const handleKeyDown = (e) => {
        // Shift + Enter를 누르면 줄바꿈
        if (e.key === 'Enter' && e.shiftKey) {
            // 기본 동작(줄바꿈)을 그대로 실행하도록 둡니다.
        }
        // Enter만 누르면 메시지 전송
        else if (e.key === 'Enter') {
            e.preventDefault(); // textarea의 기본 동작(줄바꿈)을 막습니다.
            handleSendMessage(e);
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

    // ✅ 2. 파일 관련 함수들을 추가합니다.

    // 파일 선택 창을 여는 함수
    const handleFileSelectClick = () => {
        fileInputRef.current.click();
    };

    // 파일이 선택되었을 때 실행 (이미지 미리보기, 여러 파일 처리 기능 추가)
    const handleFileChange = (event) => {
        const newFiles = Array.from(event.target.files);
        if (newFiles.length === 0) return;

        const filePromises = newFiles.map(file => {
            return new Promise((resolve) => {
                if (file.type.startsWith('image/')) {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        resolve({ file, previewUrl: e.target.result });
                    };
                    reader.readAsDataURL(file);
                } else {
                    resolve({ file, previewUrl: null });
                }
            });
        });

        Promise.all(filePromises).then(newFileObjects => {
            // ✅ 기존 파일 목록(...filesToUpload)에 새 파일 목록을 이어 붙입니다.
            setFilesToUpload(prevFiles => [...prevFiles, ...newFileObjects]);
        });

        event.target.value = null;
    };

    // ✅ 개별 파일을 삭제하는 함수 추가
    const handleRemoveFile = (fileToRemove) => {
        setFilesToUpload(prevFiles => prevFiles.filter(item => item.file !== fileToRemove));
    };

    // 파일 미리보기를 닫는 함수
    const cancelFileUpload = () => {
        setFilesToUpload([]);
    };

    // 파일 업로드 실행 (여러 파일 동시 처리)
    const handleFileUpload = async () => {
        if (filesToUpload.length === 0 || isUploading) return;

        setIsUploading(true);

        // for...of 루프와 await 키워드를 사용해 각 파일을 순차적으로 업로드합니다.
        for (const item of filesToUpload) {
            const formData = new FormData();
            formData.append('file', item.file);
            try {
                // 이 await 구문이 현재 파일의 업로드가 끝날 때까지 다음 파일로 넘어가지 않도록 막아줍니다.
                await axiosInstance.post(`/room/${roomId}/file`, formData);
            } catch (error) {
                console.error(`${item.file.name} 업로드 실패:`, error);
                alert(`${item.file.name} 업로드에 실패했습니다. 업로드를 중단합니다.`);
                // 하나라도 실패하면 즉시 반복문을 중단합니다.
                break;
            }
        }

        setIsUploading(false);
        cancelFileUpload();
    };


    if (!user) { return <h2>로그인이 필요합니다.</h2>; }

    return (
        // 최상위 div에는 별도 스타일이 없습니다.
        <div className="chat-page-container">
            <div data-id="chat-main-flex-container">
                {isProfileModalOpen && (
                    <UserProfileModal
                        profile={selectedProfile}
                        onClose={closeProfileModal}
                        position={modalPosition}
                    />
                )}
                {/* 👇 사용자 목록 패널이 모달의 기준점이 됩니다. */}
                <div
                    data-id="user-list-panel"
                    style={{
                        position: 'relative',
                        width: '200px',
                        borderRight: '1px solid #ccc',
                        padding: '10px',
                        overflowY: 'auto',
                        display: 'flex', // ✅ 세로 정렬을 위해 flex 추가
                        flexDirection: 'column', // ✅ 세로 정렬을 위해 flex 추가
                    }}
                >
                    <h2 className="panel-title">{roomName || `채팅방 #${roomId}`}</h2>
                    <h4>멤버 목록 ( {users.filter(u => u.conn === 'CONNECT').length} / {users.length} )</h4>
                    <ul style={{ listStyle: 'none', padding: 0, flexGrow: 1 }}> {/* ✅ 목록이 남은 공간을 채우도록 */}
                        {users.map(u => (
                            <li
                                key={u.userId}
                                className={`user-list-item ${u.userId === user.userId ? 'me' : ''} ${u.conn === 'DISCONNECT' ? 'disconnected' : ''}`}
                                onClick={(event) => handleUserClick(u.userId, event)}
                            >
                                {/* ✅ 1. 프로필 이미지를 표시하는 img 태그 */}
                                <img
                                    src={`${SERVER_URL}${u.profileImageUrl}`}
                                    alt={u.nickname}
                                    className="user-list-profile-img"
                                />
                                {/* ✅ 2. 닉네임을 표시하는 span 태그 */}
                                <span className="user-list-nickname">
                                    {u.nickname}
                                </span>
                            </li>
                        ))}
                    </ul>

                    {/* ✅ 5. 닉네임 변경 입력창 UI 추가 */}
                    <div className="nickname-editor">
                        <hr />
                        <input
                            type="text"
                            value={myNickname}
                            onChange={(e) => setMyNickname(e.target.value)}
                            onBlur={handleNicknameUpdate} // 포커스를 잃었을 때
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    e.preventDefault();
                                    handleNicknameUpdate(); // 엔터 키를 눌렀을 때
                                    e.target.blur(); // 입력창 포커스 해제
                                }
                            }}
                        />
                    </div>

                    {/* 모달을 기준점 내부에 렌더링합니다. */}
                    {isProfileModalOpen && (
                        <UserProfileModal
                            profile={selectedProfile}
                            onClose={closeProfileModal}
                            position={modalPosition}
                        />
                    )}
                </div>

                {/* 👇 이 채팅 영역은 이제 영향을 받지 않습니다. */}
                <div className="chat-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <div ref={scrollContainerRef} className="chat-message-list" style={{ flex: 1, overflowY: 'auto', padding: '10px', borderBottom: '1px solid #ccc' }}>
                        {isLoading && <div style={{ textAlign: 'center' }}>이전 대화 불러오는 중...</div>}
                        {hasMore && <div ref={sentinelRef} style={{ height: '1px' }} />}
                        {messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)}
                    </div>
                    {/* ✅ 이 파일 미리보기 UI 부분만 수정합니다. */}
                    {filesToUpload.length > 0 && (
                        // ✅ 미리보기 UI 구조 변경
                        <div className="file-preview-container">
                            <div className="file-preview-list">
                                {filesToUpload.map((item, index) => (
                                    <div key={index} className="file-preview-item">
                                        {item.previewUrl ? (
                                            <>
                                                <img src={item.previewUrl} alt={item.file.name} className="image-preview-thumbnail" />
                                                <span className="file-preview-name">{item.file.name}</span>
                                            </>
                                        ) : (
                                            <span>📄 {item.file.name}</span>
                                        )}
                                        {/* ✅ 개별 삭제 버튼 추가 */}
                                        <button
                                            className="remove-file-btn"
                                            onClick={() => handleRemoveFile(item.file)}
                                        >
                                            &times; {/* 'x' 모양 문자 */}
                                        </button>
                                    </div>
                                ))}
                            </div>
                            {/* ✅ 전송/취소 버튼이 이제 파일 목록과 같은 레벨에 위치합니다. */}
                            <div className="file-preview-actions">
                                <button onClick={handleFileUpload}>전송 ({filesToUpload.length}개)</button>
                                <button onClick={cancelFileUpload}>취소</button>
                            </div>
                        </div>
                    )}
                    {/* ✅ 4. 메시지 입력 폼을 수정합니다. */}
                    <form onSubmit={handleSendMessage} className="chat-input-form" style={{ padding: '10px', display: 'flex', alignItems: 'center' }}>
                        {/* 숨겨진 실제 파일 입력창 */}
                        <input
                            type="file"
                            multiple
                            ref={fileInputRef}
                            onChange={handleFileChange}
                            style={{ display: 'none' }}
                        />
                        {/* 파일 선택을 위한 커스텀 버튼 */}
                        <button type="button" onClick={handleFileSelectClick} className="file-select-button">
                            📎
                        </button>

                        <textarea
                            ref={textareaRef}
                            className="chat-textarea"
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder="메시지 입력..."
                            rows={1} // 시작은 한 줄로
                        />
                        <button type="submit" className="send-button">
                            <IoSend />
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default ChatPage;