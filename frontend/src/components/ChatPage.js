import React, { useEffect, useContext, useState, useRef, useLayoutEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom'; // 1. useNavigate 임포트
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import { RoomContext } from '../context/RoomContext'; // Import RoomContext
import { ModalContext } from '../context/ModalContext';
import ChatMessage from './ChatMessage';
import UserProfileModal from './UserProfileModal';
import { toast } from 'react-toastify';
import { IoSend } from "react-icons/io5";
import { FaUsers } from 'react-icons/fa';
import axiosInstance from '../api/axiosInstance';
const SERVER_URL = axiosInstance.getUri();

function ChatPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const { openUserProfileModal, toggleFriendListModal, closeFriendListModal } = useContext(ModalContext);
    const { activeRoomId, setActiveRoomId, joinedRooms, exitRoom, deleteRoom } = useContext(RoomContext);
    const { messagesByRoom, usersByRoom, stompClientsRef, isRoomLoading, loadMoreMessages, hasMoreMessagesByRoom } = useContext(ChatContext);

    // --- UI 상호작용을 위한 Local State ---
    const [newMessage, setNewMessage] = useState('');
    const [myNickname, setMyNickname] = useState('');
    const [filesToUpload, setFilesToUpload] = useState([]);
    const [isUploading, setIsUploading] = useState(false);
    const [isDragging, setIsDragging] = useState(false);
    const [myRole, setMyRole] = useState(null);
    const [isFetchingMore, setIsFetchingMore] = useState(false);
    const [isUserScrolling, setIsUserScrolling] = useState(false);
    const scrollTimeoutRef = useRef(null);
    const [isUserListVisible, setIsUserListVisible] = useState(window.innerWidth > 768);


    // --- DOM 참조 및 스크롤 관리를 위한 Ref ---
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);
    const inviteButtonRef = useRef(null);
    const messagesEndRef = useRef(null);

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
    const hasMoreMessages = hasMoreMessagesByRoom[currentRoomId] !== false;
    
    useEffect(() => {
        const handleResize = () => {
            setIsUserListVisible(window.innerWidth > 768);
        };
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // 4. 방 나가기/삭제 핸들러 함수 추가
    const handleExitRoom = () => {
        if (window.confirm("정말로 이 방에서 나가시겠습니까?")) {
            exitRoom(currentRoomId);
            navigate('/'); // 로비로 이동
        }
    };

    const handleDeleteRoom = () => {
        if (window.confirm("정말로 이 방을 삭제하시겠습니까? 모든 대화 내용이 사라집니다.")) {
            deleteRoom(currentRoomId);
            navigate('/'); // 로비로 이동
        }
    };

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
            await axiosInstance.post(`/room/${roomId}/invite`, { userId: friend.userId });
            toast.success(`${friend.nickname}님을 방에 초대했습니다!`);
            closeFriendListModal();
        } catch (error) {
            const errorMessage = error.response?.data?.message || '초대에 실패했습니다. 다시 시도해주세요.';
            toast.error(errorMessage);
            console.error("Failed to invite friend:", error);
        }
    };
    
    const handleOpenInviteModal = () => {
        const rect = inviteButtonRef.current.getBoundingClientRect();
        toggleFriendListModal({
            title: '친구 초대하기',
            onFriendClick: handleInviteFriend,
            position: {
                mode: 'fixed',
                bottom: window.innerHeight - rect.top + 5,
                left: rect.left - 10
            }
        });
    };
    
    useEffect(() => {
        if (isFetchingMore && !hasMoreMessages) {
            setIsFetchingMore(false);
        }
    }, [isFetchingMore, hasMoreMessages]);

    // --- Effects ---
    useEffect(() => {
        const currentRoomId = Number(roomId);
        setActiveRoomId(currentRoomId);
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
    
    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (!container) return;

        if (isFetchingMore) {
            // 이전 메시지 로딩이 완료된 시점.
            // 스크롤 위치를 보존한 후, 로딩 상태를 해제합니다.
            container.scrollTop = container.scrollHeight - prevScrollHeightRef.current;
            setIsFetchingMore(false);
        } else {
            // 새 메시지 수신 또는 초기 로딩 시점.
            // 스크롤이 맨 아래 근처에 있었을 때만 맨 아래로 이동시킵니다.
            // prevScrollHeightRef.current가 null이면 초기 로딩이므로 무조건 맨 아래로 갑니다.
            const wasAtBottom = prevScrollHeightRef.current ? (container.scrollTop + container.clientHeight >= prevScrollHeightRef.current - 20) : true;
            if (wasAtBottom) {
                container.scrollTop = container.scrollHeight;
            }
        }
        // 다음 렌더링을 위해 현재 스크롤 높이를 기록합니다.
        prevScrollHeightRef.current = container.scrollHeight;
    }, [messages]);

    useEffect(() => {
        if (user && users.length > 0) {
            const me = users.find(u => u.userId === user.userId);
            if (me) {
                setMyNickname(me.nickname);
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
            if (filesToUpload.length > 0) {
                handleFileUpload();
            } else {
                handleSendMessage(e);
            }
        }
    };

    const handleScroll = () => {
        setIsUserScrolling(true);
        clearTimeout(scrollTimeoutRef.current);
        scrollTimeoutRef.current = setTimeout(() => {
            setIsUserScrolling(false);
        }, 150);

        const container = scrollContainerRef.current;
        const hasMore = hasMoreMessagesByRoom[currentRoomId] !== false;

        if (container && container.scrollTop < 1 && !isFetchingMore && hasMore) {
            // 현재 스크롤 높이를 저장하고, 로딩 상태를 true로 설정합니다.
            // 로딩 상태 해제는 useLayoutEffect에서 처리합니다.
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
        event.target.value = null;
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
                toast.error(`${item.file.name} 업로드에 실패했습니다.`);
                break;
            }
        }
        setIsUploading(false);
        setFilesToUpload([]);
    };
    
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
    }, [addFiles]);
    
    const handleDragOver = (event) => {
        event.preventDefault();
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
    
    const handleUserClick = async (clickedUserId, event) => {
        const liRect = event.currentTarget.getBoundingClientRect();
        const containerRect = event.currentTarget.closest('[data-id="chat-main-flex-container"]').getBoundingClientRect();
        const position = {
            top: liRect.top,
            left: liRect.left,
        };
        try {
            const response = await axiosInstance.get(`/user/${clickedUserId}/profile`);
            openUserProfileModal(response.data, position);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            toast.error('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };
    const handleRemoveFile = (fileToRemove) => {
        setFilesToUpload(prevFiles => prevFiles.filter(item => item.file !== fileToRemove));
    };
    
    useEffect(() => {
        window.addEventListener('paste', handlePaste);
        return () => {
            window.removeEventListener('paste', handlePaste);
        };
    }, [handlePaste]);
    
    return (
        <div
            className={`chat-page-container ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <div data-id="chat-main-flex-container" className="chat-main-flex-container">
                {isUserListVisible && (
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
                        <button
                            ref={inviteButtonRef}
                            onClick={handleOpenInviteModal}
                            data-modal-toggle="friendlist"
                        >친구 초대</button>
                        <div className="nickname-editor">
                            <input type="text" value={myNickname} onChange={(e) => setMyNickname(e.target.value)} onBlur={handleNicknameUpdate}
                                   onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleNicknameUpdate(); e.target.blur(); }}}/>
                        </div>
                    </div>
                )}
                <div className="chat-panel">
                    <div className="chat-panel-header">
                        <button 
                            className="toggle-user-list-btn" 
                            onClick={() => setIsUserListVisible(!isUserListVisible)}
                        >
                            <FaUsers />
                        </button>
                        {/* 5. 방 나가기/삭제 버튼 JSX 추가 */}
                        <div className="room-actions">
                            {myRole !== 'ADMIN' && (
                                <button onClick={handleExitRoom}>방 나가기</button>
                            )}
                            {myRole === 'ADMIN' && (
                                <button onClick={handleDeleteRoom} className="danger-button">
                                    방 삭제
                                </button>
                            )}
                        </div>
                    </div>
                    <div
                        ref={scrollContainerRef}
                        className={`chat-message-list ${isUserScrolling ? 'is-scrolling' : ''}`}
                        onScroll={handleScroll}
                    >
                        {isLoading ? (
                            <div style={{ textAlign: 'center', padding: '20px' }}>
                                채팅 내역을 불러오는 중입니다...
                            </div>
                        ) : (
                            <>
                                {!hasMoreMessages && (
                                    <div style={{ textAlign: 'center', padding: '10px', color: '#888' }}>
                                        대화의 시작입니다.
                                    </div>
                                )}
                                {isFetchingMore && <div style={{ textAlign: 'center', padding: '10px' }}>이전 메시지 로딩 중...</div>}
                                {messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)}
                                <div ref={messagesEndRef} />
                            </>
                        )}
                    </div>
                    {filesToUpload.length > 0 && (
                        <div className="file-preview-container">
                            <div className="file-preview-list">
                                {filesToUpload.map((item, index) => (
                                    <div key={index} className="file-preview-item">
                                        <img
                                            src={item.previewUrl || '/default-file-icon.png'}
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