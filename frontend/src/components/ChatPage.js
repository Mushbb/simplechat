import React, { useEffect, useContext, useState, useRef, useLayoutEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import ChatMessage from './ChatMessage';
import UserProfileModal from './UserProfileModal';
import { IoSend } from "react-icons/io5";
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = 'http://10.50.131.25:8080';

function ChatPage() {
    const { roomId } = useParams();
    const { user, openFriendListModal, closeFriendListModal, friendModalConfig, openUserProfileModal } = useContext(AuthContext);
    const { setActiveRoomId, messagesByRoom, usersByRoom, joinedRooms, stompClientsRef, isRoomLoading, loadMoreMessages, hasMoreMessagesByRoom } = useContext(ChatContext);

    // --- UI 상호작용을 위한 Local State ---
    const [newMessage, setNewMessage] = useState('');
    const [myNickname, setMyNickname] = useState('');
    const [filesToUpload, setFilesToUpload] = useState([]);
    const [isUploading, setIsUploading] = useState(false);
    const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });
    const [isDragging, setIsDragging] = useState(false);
    const [myRole, setMyRole] = useState(null);
    const [isFetchingMore, setIsFetchingMore] = useState(false);
    const [isUserScrolling, setIsUserScrolling] = useState(false);
    const scrollTimeoutRef = useRef(null);

    // --- DOM 참조 및 스크롤 관리를 위한 Ref ---
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);

    const currentRoomId = Number(roomId);
    const roomName = joinedRooms.find(r => r.id === currentRoomId)?.name || '';
    const messages = messagesByRoom[currentRoomId] || [];
    const users = usersByRoom[currentRoomId] || [];
    // 사용자 목록을 정렬하는 로직
    const sortedUsers = [...users].sort((a, b) => {
        // 1. 접속 상태로 정렬 (온라인이 위로)
        if (a.conn === 'CONNECT' && b.conn !== 'CONNECT') return -1;
        if (a.conn !== 'CONNECT' && b.conn === 'CONNECT') return 1;
        
        // 2. 역할로 정렬 (방장(ADMIN)이 위로)
        if (a.role === 'ADMIN' && b.role !== 'ADMIN') return -1;
        if (a.role !== 'ADMIN' && b.role === 'ADMIN') return 1;
        
        // 3. 닉네임 오름차순으로 정렬
        return a.nickname.localeCompare(b.nickname);
    });
    const scrollActionRef = useRef('initial');
    const isLoading = isRoomLoading[currentRoomId] !== false;
    // ✅ NEW: 현재 방에 더 불러올 메시지가 있는지 확인하는 변수
    // 아직 값이 설정되지 않았다면 기본값으로 true를 사용합니다.
    const hasMoreMessages = hasMoreMessagesByRoom[currentRoomId] !== false;
    
    // ✅ addFiles 함수를 useCallback으로 감싸줍니다.
    const addFiles = useCallback((newFiles) => {
        if (newFiles.length === 0) return;
        const filesArray = Array.from(newFiles);
        const filePromises = filesArray.map(file => new Promise((resolve) => {
            if (file.type.startsWith('image/')) {
                const reader = new FileReader();
                reader.onload = (e) => resolve({ file, previewUrl: e.target.result });
                reader.readAsDataURL(file);
            } else {
                resolve({ file, previewUrl: null });
            }
        }));
        Promise.all(filePromises).then(newFileObjects => {
            setFilesToUpload(prevFiles => [...prevFiles, ...newFileObjects]);
            textareaRef.current?.focus();
        });
    }, []); // 의존성 배열이 비어있어도 괜찮습니다. (setFilesToUpload는 항상 동일)
    
    const handleInviteFriend = async (friend) => {
        try {
            // 1. 서버에 친구를 방으로 초대하는 API를 호출합니다.
            //    요청 주소: /room/{roomId}/invite
            //    요청 내용: { userId: 초대할 친구의 ID }
            await axiosInstance.post(`/room/${roomId}/invite`, { userId: friend.userId });
            
            // 2. API 호출이 성공하면 알림을 띄웁니다.
            alert(`${friend.nickname}님을 방에 초대했습니다!`);
            
            // 3. 작업이 끝났으므로 친구 목록 모달을 닫습니다.
            closeFriendListModal();
            
        } catch (error) {
            // 4. API 호출이 실패하면 서버가 보낸 에러 메시지를 띄웁니다.
            //    (예: "이미 참여하고 있는 사용자입니다.")
            const errorMessage = error.response?.data?.message || '초대에 실패했습니다. 다시 시도해주세요.';
            alert(errorMessage);
            console.error("Failed to invite friend:", error);
        }
    };
    
    const handleOpenInviteModal = () => {
        friendModalConfig.isOpen = true;
        openFriendListModal({
            title: '친구 초대하기',
            onFriendClick: handleInviteFriend
        });
    };
    
    // ✅ MODIFIED: 'stuck loading' 버그를 해결하기 위한 useEffect
    // 이전에 제안했던 prevMessageCountRef 로직을 이 방식으로 대체하거나 통합합니다.
    useEffect(() => {
        // 로딩 중 상태인데, 더 이상 불러올 메시지가 없다고 판명되면 로딩 상태를 해제합니다.
        if (isFetchingMore && !hasMoreMessages) {
            setIsFetchingMore(false);
        }
    }, [isFetchingMore, hasMoreMessages]);
    
    // ✅ 탭 전환 시 스크롤을 맨 아래로 내리는 전용 Effect를 추가합니다.
    useEffect(() => {
        // setTimeout을 사용하여 브라우저가 이미지 렌더링을 시작할 시간을 줍니다.
        // 딜레이를 0으로 주어도, 실행 순서를 한 틱 뒤로 미루는 효과가 있습니다.
        const timer = setTimeout(() => {
            const container = scrollContainerRef.current;
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        }, 50); // 아주 짧은 딜레이 (0~100ms)

        // 다른 방으로 이동하기 전에 타이머를 정리합니다 (메모리 누수 방지)
        return () => clearTimeout(timer);

    }, [roomId]); // ✅ 오직 roomId가 바뀔 때(탭을 전환할 때)만 실행됩니다.

    // --- Effects ---
    useEffect(() => {
        const currentRoomId = Number(roomId);
        setActiveRoomId(currentRoomId);

        // ✅ 방을 바꿀 때마다 스크롤 액션을 'initial'로 리셋합니다.
        scrollActionRef.current = 'initial';
    }, [currentRoomId, setActiveRoomId]);

    useEffect(() => {
        if (user && users.length > 0) {
            const me = users.find(u => u.userId === user.userId);
            if (me) setMyNickname(me.nickname);
        }
    }, [users, user]);

    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = '0px';
            const scrollHeight = textarea.scrollHeight;
            textarea.style.height = `${scrollHeight}px`;
        }
    }, [newMessage]);
    
    // ✅ MODIFIED: 기존 useLayoutEffect를 수정하여 스크롤 위치를 보존하는 로직을 추가합니다.
    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (!container) return;
        
        if (isFetchingMore) {
            // 이전 메시지가 로드되었을 때: 스크롤 위치를 조정하여 뷰를 유지
            container.scrollTop = container.scrollHeight - prevScrollHeightRef.current;
            setIsFetchingMore(false); // 로딩 상태 해제
        } else {
            // 새 메시지가 오거나 방에 처음 들어왔을 때: 맨 아래로 스크롤
            container.scrollTop = container.scrollHeight;
        }
    }, [messages]); // messages 배열이 변경될 때마다 실행
    
    useEffect(() => {
        if (user && users.length > 0) {
            const me = users.find(u => u.userId === user.userId);
            if (me) {
                setMyNickname(me.nickname);
                // ✅ 2. 내 역할 정보도 함께 state에 저장
                setMyRole(me.role);
            }
        }
    }, [users, user]);

    // --- 이벤트 핸들러 ---
    const handleSendMessage = (e) => {
        e.preventDefault();
        const messageContent = newMessage.trim();
        const client = stompClientsRef.current.get(currentRoomId);
        if (messageContent && client?.connected) {
            const chatMessage = { roomId: currentRoomId, authorId: user.userId, content: messageContent, messageType: 'TEXT' };
            client.publish({ destination: '/app/chat.sendMessage', body: JSON.stringify(chatMessage) });
            setNewMessage('');
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && e.shiftKey) return;
        if (e.key === 'Enter') {
            e.preventDefault();
            // 업로드할 파일이 있는지 먼저 확인합니다.
            if (filesToUpload.length > 0) {
                // 파일이 있으면, 텍스트 내용은 무시하고 파일만 전송합니다.
                handleFileUpload();
            } else {
                // 파일이 없으면, 기존처럼 텍스트 메시지를 전송합니다.
                handleSendMessage(e);
            }
        }
    };
    
    const handleScroll = () => {
        // 스크롤 시작 시 상태 변경 및 타이머 설정
        setIsUserScrolling(true);
        clearTimeout(scrollTimeoutRef.current);
        scrollTimeoutRef.current = setTimeout(() => {
            setIsUserScrolling(false);
        }, 150);
        
        // 이전 메시지 로딩 로직
        const container = scrollContainerRef.current;
        const hasMoreMessages = hasMoreMessagesByRoom[currentRoomId] !== false;
        if (container && container.scrollTop === 0 && !isFetchingMore && hasMoreMessages) {
            prevScrollHeightRef.current = container.scrollHeight;
            setIsFetchingMore(true);
            loadMoreMessages(currentRoomId);
        }
    };

    const handleNicknameUpdate = () => {
        const me = users.find(u => u.userId === user.userId);
        if (!me || me.nickname === myNickname || myNickname.trim() === '') {
            if(me) setMyNickname(me.nickname);
            return;
        }
        const client = stompClientsRef.current.get(currentRoomId);
        if (client?.connected) {
            const nickChangeMessage = { roomId: currentRoomId, userId: user.userId, newNickname: myNickname.trim() };
            client.publish({ destination: '/app/chat.changeNick', body: JSON.stringify(nickChangeMessage) });
        }
    };
    
    const handleFileChange = (event) => {
        addFiles(event.target.files);
        event.target.value = null; // 같은 파일을 다시 선택할 수 있도록 초기화
    };

    const handleFileUpload = async () => {
        if (filesToUpload.length === 0 || isUploading) return;
        setIsUploading(true);
        for (const item of filesToUpload) {
            const formData = new FormData();
            formData.append('file', item.file);
            try {
                await axiosInstance.post(`/room/${currentRoomId}/file`, formData);
            } catch (error) {
                alert(`${item.file.name} 업로드에 실패했습니다.`);
                break;
            }
        }
        setIsUploading(false);
        setFilesToUpload([]);
    };
    
    // ✅ handlePaste 함수도 useCallback으로 감싸줍니다.
    const handlePaste = useCallback((event) => {
        const items = event.clipboardData.items;
        const imageFiles = [];
        for (let i = 0; i < items.length; i++) {
            if (items[i].kind === 'file' && items[i].type.startsWith('image/')) {
                const file = items[i].getAsFile();
                const fileName = `clipboard_image_${Date.now()}.png`;
                imageFiles.push(new File([file], fileName, { type: file.type }));
            }
        }
        if (imageFiles.length > 0) {
            event.preventDefault();
            addFiles(imageFiles);
        }
    }, [addFiles]); // addFiles 함수에 의존합니다.
    
    const handleDragOver = (event) => {
        event.preventDefault(); // 브라우저 기본 동작(파일 열기) 방지
        setIsDragging(true);
    };
    
    const handleDragLeave = (event) => {
        event.preventDefault();
        setIsDragging(false);
    };
    
    const handleDrop = (event) => {
        event.preventDefault();
        setIsDragging(false);
        addFiles(event.dataTransfer.files);
    };
    
    
    // --- 사용자 클릭 및 모달 위치 계산 로직 수정 ---
    const handleUserClick = async (clickedUserId, event) => {
        // 클릭된 li 요소의 화면상 위치 정보를 가져옵니다.
        const liRect = event.currentTarget.getBoundingClientRect();
        // 기준점이 될 컨테이너의 화면상 위치 정보를 가져옵니다.
        const containerRect = event.currentTarget.closest('[data-id="chat-main-flex-container"]').getBoundingClientRect();
        
        // 컨테이너를 기준으로 모달이 표시될 상대 위치를 계산합니다.
        const position = {
            top: liRect.top,
            left: liRect.left,
        };
        // setModalPosition(position);
        
        try {
            const response = await axiosInstance.get(`/user/${clickedUserId}/profile`);
            openUserProfileModal(response.data, position);
            // setSelectedProfile(response.data);
            // setIsProfileModalOpen(true);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            alert('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };
    const handleRemoveFile = (fileToRemove) => {
        setFilesToUpload(prevFiles => prevFiles.filter(item => item.file !== fileToRemove));
    };
    
    useEffect(() => {
        // 전역(window)에서 paste 이벤트가 발생하면 handlePaste 함수를 호출합니다.
        window.addEventListener('paste', handlePaste);
        
        // 컴포넌트가 화면에서 사라질 때(unmount) 이벤트 리스너를 제거합니다.
        // (메모리 누수 방지를 위해 매우 중요합니다.)
        return () => {
            window.removeEventListener('paste', handlePaste);
        };
    }, [handlePaste]); // handlePaste 함수가 변경될 때만 이 effect를 재실행합니다.
    
    return (
        <div
            className={`chat-page-container ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <div data-id="chat-main-flex-container" className="chat-main-flex-container">
                {isProfileModalOpen && (
                    <UserProfileModal profile={selectedProfile} onClose={() => setIsProfileModalOpen(false)} position={modalPosition} />
                )}
                <div data-id="user-list-panel" className="user-list-panel">
                    <h2 className="panel-title">{roomName}</h2>
                    <h4>멤버 목록 ({users.filter(u => u.conn === 'CONNECT').length} / {users.length})</h4>
                    <ul className="user-list-scrollable">
                        {sortedUsers.map(u => (
                            <li key={u.userId} className={`user-list-item ${u.userId === user.userId ? 'me' : ''} ${u.conn === 'DISCONNECT' ? 'disconnected' : ''} ${u.role === 'ADMIN' ? 'admin' : ''}`}
                                onClick={(event) => handleUserClick(u.userId, event)}>
                                <img src={`${SERVER_URL}${u.profileImageUrl}`} alt={u.nickname} className="user-list-profile-img" />
                                <span className="user-list-nickname">{u.nickname}</span>
                            </li>
                        ))}
                    </ul>
                    <button onClick={handleOpenInviteModal}>친구 초대</button>
                    <div className="nickname-editor">
                        <input type="text" value={myNickname} onChange={(e) => setMyNickname(e.target.value)} onBlur={handleNicknameUpdate}
                               onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleNicknameUpdate(); e.target.blur(); }}}/>
                    </div>
                </div>
                <div className="chat-panel">
                    {/* ✅ MODIFIED: isUserScrolling 상태에 따라 클래스를 동적으로 추가합니다. */}
                    <div
                        ref={scrollContainerRef}
                        className={`chat-message-list ${isUserScrolling ? 'is-scrolling' : ''}`}
                        onScroll={handleScroll}
                    >
                        {/* ✅ 3. 로딩 상태에 따라 조건부 렌더링 */}
                        {isLoading ? (
                            <div style={{ textAlign: 'center', padding: '20px' }}>
                                채팅 내역을 불러오는 중입니다...
                            </div>
                        ) : (
                            <>
                                {/* ✅ MODIFIED: 조건부 렌더링 로직 수정 */}
                                {!hasMoreMessages && (
                                    <div style={{ textAlign: 'center', padding: '10px', color: '#888' }}>
                                        대화의 시작입니다.
                                    </div>
                                )}
                                {isFetchingMore && <div style={{ textAlign: 'center', padding: '10px' }}>이전 메시지 로딩 중...</div>}
                                {messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)}
                            </>
                        )}
                    </div>
                    {filesToUpload.length > 0 && (
                        <div className="file-preview-container">
                            <div className="file-preview-list">
                                {filesToUpload.map((item, index) => (
                                    <div key={index} className="file-preview-item">
                                        <img
                                            src={item.previewUrl || '/default-file-icon.png'} // 이미지가 아닐 경우를 대비한 기본 아이콘 경로
                                            alt={item.file.name}
                                            className="image-preview-thumbnail"
                                        />
                                        <span className="file-preview-name">{item.file.name}</span>
                                        <button
                                            onClick={() => handleRemoveFile(item.file)}
                                            className="remove-file-btn"
                                        >
                                            &times;
                                        </button>
                                    </div>
                                ))}
                            </div>
                            <div className="file-preview-actions">
                                <button onClick={handleFileUpload} disabled={isUploading}>
                                    {isUploading ? '업로드 중...' : `전송 (${filesToUpload.length})`}
                                </button>
                                <button onClick={() => setFilesToUpload([])} className="danger-button">
                                    모두 취소
                                </button>
                            </div>
                        </div>
                    )}
                    <form onSubmit={handleSendMessage} className="chat-input-form">
                        <input type="file" multiple ref={fileInputRef} onChange={handleFileChange} style={{ display: 'none' }} />
                        <button type="button" onClick={() => fileInputRef.current.click()} className="file-select-button">📎</button>
                        <textarea ref={textareaRef} className="chat-textarea" value={newMessage} onChange={(e) => setNewMessage(e.target.value)} onKeyDown={handleKeyDown} placeholder="메시지 입력..." rows={1} />
                        <button type="submit" className="send-button"><IoSend /></button>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default ChatPage;