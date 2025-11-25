import React, { useEffect, useContext, useState, useRef, useLayoutEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import { RoomContext } from '../context/RoomContext';
import { ModalContext } from '../context/ModalContext';
import ChatMessage from './ChatMessage';
import UserProfileModal from './UserProfileModal';
import { toast } from 'react-toastify';
import { IoSend } from "react-icons/io5";
import { FaUsers } from 'react-icons/fa';
import axiosInstance from '../api/axiosInstance';
import '../styles/ChatPage.css';

/**
 * @file 특정 채팅방의 전체 UI와 상호작용을 담당하는 메인 페이지 컴포넌트입니다.
 * 메시지 목록, 사용자 목록, 메시지 입력, 파일 전송 등 채팅에 필요한 모든 기능을 포함합니다.
 */

const SERVER_URL = axiosInstance.getUri();

/**
 * 채팅방 페이지 컴포넌트.
 * @returns {JSX.Element} ChatPage 컴포넌트의 JSX.
 */
function ChatPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();

    // --- Contexts ---
    const { user } = useContext(AuthContext);
    const { openUserProfileModal, toggleFriendListModal, closeFriendListModal } = useContext(ModalContext);
    const { activeRoomId, setActiveRoomId, joinedRooms, exitRoom, deleteRoom } = useContext(RoomContext);
    const { messagesByRoom, usersByRoom, stompClientsRef, isRoomLoading, loadMoreMessages, hasMoreMessagesByRoom } = useContext(ChatContext);

    // --- Local UI State ---
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 메시지 입력창의 현재 텍스트 */
    const [newMessage, setNewMessage] = useState('');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 현재 방에서 사용자의 닉네임 */
    const [myNickname, setMyNickname] = useState('');
    /** @type {[Array<{file: File, previewUrl: string|null}>, React.Dispatch<React.SetStateAction<Array<{file: File, previewUrl: string|null}>>>]} 업로드할 파일 목록과 미리보기 URL */
    const [filesToUpload, setFilesToUpload] = useState([]);
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 파일 업로드 진행 상태 */
    const [isUploading, setIsUploading] = useState(false);
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 파일 드래그 앤 드롭 활성화 상태 */
    const [isDragging, setIsDragging] = useState(false);
    /** @type {[string|null, React.Dispatch<React.SetStateAction<string|null>>]} 현재 방에서의 사용자 역할 ('ADMIN' | 'MEMBER') */
    const [myRole, setMyRole] = useState(null);
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 이전 메시지를 불러오는 중인지 여부 */
    const [isFetchingMore, setIsFetchingMore] = useState(false);
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 사용자가 스크롤 중인지 여부 */
    const [isUserScrolling, setIsUserScrolling] = useState(false);
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 사용자 목록 패널의 표시 여부 */
    const [isUserListVisible, setIsUserListVisible] = useState(window.innerWidth > 768);
    /** @type {[{x: number, y: number, user: object, items: Array}|null, React.Dispatch<React.SetStateAction<{x: number, y: number, user: object, items: Array}>>]} 컨텍스트 메뉴 상태 (위치, 대상 유저, 메뉴 아이템) */
    const [contextMenu, setContextMenu] = useState(null);
    
    // --- DOM Refs ---
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);
    const inviteButtonRef = useRef(null);
    const messagesEndRef = useRef(null);
    const contextMenuRef = useRef(null);
    const scrollTimeoutRef = useRef(null);

    // --- Derived State ---
    const currentRoomId = Number(roomId);
    const roomName = joinedRooms.find(r => r.id === currentRoomId)?.name || '';
    const messages = messagesByRoom[currentRoomId] || [];
    const users = usersByRoom[currentRoomId] || [];
    /**
     * 사용자 목록을 정렬 (온라인 > 오프라인, 관리자 > 일반, 닉네임 오름차순).
     * @type {import('../context/ChatContext').ChatUser[]}
     */
    const sortedUsers = [...users].sort((a, b) => {
        if (a.conn === 'CONNECT' && b.conn !== 'CONNECT') return -1;
        if (a.conn !== 'CONNECT' && b.conn === 'CONNECT') return 1;
        if (a.role === 'ADMIN' && b.role !== 'ADMIN') return -1;
        if (a.role !== 'ADMIN' && b.role === 'ADMIN') return 1;
        return a.nickname.localeCompare(b.nickname);
    });
    const isLoading = isRoomLoading[currentRoomId] !== false;
    const hasMoreMessages = hasMoreMessagesByRoom[currentRoomId] !== false;
    
    /**
     * 화면 크기 변경 시 사용자 목록 패널의 표시 여부를 결정하는 Effect.
     */
    useEffect(() => {
        const handleResize = () => setIsUserListVisible(window.innerWidth > 768);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    /**
     * 현재 방에서 나가는 함수.
     */
    const handleExitRoom = () => {
        if (window.confirm("정말로 이 방에서 나가시겠습니까?")) {
            exitRoom(currentRoomId);
            navigate('/');
        }
    };

    /**
     * 현재 방을 삭제하는 함수 (방장 권한 필요).
     */
    const handleDeleteRoom = () => {
        if (window.confirm("정말로 이 방을 삭제하시겠습니까? 모든 대화 내용이 사라집니다.")) {
            deleteRoom(currentRoomId);
            navigate('/');
        }
    };

    /**
     * 파일 선택, 드래그앤드롭, 붙여넣기로 추가된 파일들을 상태에 추가하고 미리보기를 생성하는 함수.
     * @param {FileList|File[]} newFiles - 새로 추가된 파일 목록.
     */
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
    }, [textareaRef]);
    
    /**
     * 친구 목록 모달에서 친구를 클릭했을 때 초대를 보내는 함수.
     * @param {import('../context/FriendContext').Friend} friend - 초대할 친구 객체.
     */
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
    
    /**
     * '친구 초대' 버튼 클릭 시 친구 목록 모달을 토글하는 함수.
     */
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
    
    /**
     * 이전 메시지를 모두 불러온 경우, 로딩 상태를 false로 설정하는 Effect.
     */
    useEffect(() => {
        if (isFetchingMore && !hasMoreMessages) {
            setIsFetchingMore(false);
        }
    }, [isFetchingMore, hasMoreMessages]);

    /**
     * 현재 방 ID를 `RoomContext`의 `activeRoomId`로 설정하는 Effect.
     */
    useEffect(() => {
        const currentRoomId = Number(roomId);
        setActiveRoomId(currentRoomId);
    }, [roomId, setActiveRoomId]);

    /**
     * 현재 방의 사용자 목록이 변경될 때 '내 닉네임'과 '내 역할' 상태를 업데이트하는 Effect.
     */
    useEffect(() => {
        if (user && users.length > 0) {
            const me = users.find(u => u.userId === user.userId);
            if (me) {
                setMyNickname(me.nickname);
                setMyRole(me.role);
            }
        }
    }, [users, user]);

    /**
     * 메시지 입력창의 내용이 변경될 때마다 높이를 자동으로 조절하는 Effect.
     */
    useEffect(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = '0px';
            const scrollHeight = textarea.scrollHeight;
            textarea.style.height = `${scrollHeight}px`;
        }
    }, [newMessage]);
    
    /**
     * 메시지 목록의 스크롤 위치를 관리하는 `useLayoutEffect`.
     * - 이전 메시지를 불러왔을 때: 스크롤 위치를 유지.
     * - 새 메시지를 받았을 때: 사용자가 스크롤을 올리지 않았다면 맨 아래로 스크롤.
     * - 초기 로딩 시: 맨 아래로 스크롤.
     */
    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (!container) return;

        if (isFetchingMore) {
            container.scrollTop = container.scrollHeight - prevScrollHeightRef.current;
            setIsFetchingMore(false);
        } else {
            const wasAtBottom = prevScrollHeightRef.current ? (container.scrollTop + container.clientHeight >= prevScrollHeightRef.current - 20) : true;
            if (wasAtBottom) {
                container.scrollTop = container.scrollHeight;
            }
        }
        prevScrollHeightRef.current = container.scrollHeight;
    }, [messages, isFetchingMore]);

    /**
     * 메시지 전송 폼 제출 시 호출되는 핸들러.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
    const handleSendMessage = (e) => {
        e.preventDefault();
        const messageContent = newMessage.trim();
        const client = stompClientsRef.current.get(currentRoomId);
        if (messageContent && client?.connected) {
            const mentionedUserIds = [];
            const mentionRegex = /@([^\s]+)/g;
            let match;
            const currentRoomUsers = usersByRoom[currentRoomId] || [];

            while ((match = mentionRegex.exec(messageContent)) !== null) {
                const mentionedNickname = match[1];
                const mentionedUser = currentRoomUsers.find(u => u.nickname === mentionedNickname);
                if (mentionedUser) {
                    mentionedUserIds.push(mentionedUser.userId);
                }
            }

            const chatMessage = { 
                roomId: currentRoomId, 
                authorId: user.userId, 
                content: messageContent, 
                messageType: 'TEXT',
                mentionedUserIds: mentionedUserIds
            };
            client.publish({ destination: '/app/chat.sendMessage', body: JSON.stringify(chatMessage) });
            setNewMessage('');
        }
    };

    /**
     * 메시지 입력창에서 키보드 입력 이벤트를 처리하는 핸들러.
     * Shift+Enter는 줄바꿈, Enter는 메시지 전송으로 처리합니다.
     * @param {React.KeyboardEvent} e - 키보드 이벤트.
     */
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

    /**
     * 메시지 목록 스크롤 시 호출되는 핸들러.
     * 스크롤이 최상단에 도달하면 이전 메시지를 불러옵니다.
     */
    const handleScroll = () => {
        setIsUserScrolling(true);
        clearTimeout(scrollTimeoutRef.current);
        scrollTimeoutRef.current = setTimeout(() => setIsUserScrolling(false), 150);

        const container = scrollContainerRef.current;
        const hasMore = hasMoreMessagesByRoom[currentRoomId] !== false;

        if (container && container.scrollTop < 1 && !isFetchingMore && hasMore) {
            prevScrollHeightRef.current = container.scrollHeight;
            setIsFetchingMore(true);
            loadMoreMessages(currentRoomId);
        }
    };

    /**
     * 닉네임 입력창에서 포커스가 벗어날 때 닉네임 변경을 시도하는 핸들러.
     */
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
    
    /**
     * 파일 선택(input)이 변경되었을 때 호출되는 핸들러.
     * @param {React.ChangeEvent<HTMLInputElement>} event - 변경 이벤트.
     */
    const handleFileChange = (event) => {
        addFiles(event.target.files);
        event.target.value = null;
    };

    /**
     * '전송' 버튼 클릭 시 선택된 파일들을 서버에 업로드하는 함수.
     */
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
    
    /**
     * 채팅 입력창에 이미지 파일을 붙여넣기 했을 때 실행되는 핸들러.
     * @param {ClipboardEvent} event - 붙여넣기 이벤트.
     */
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
    
    /**
     * 파일 드래그가 영역에 들어왔을 때 실행되는 핸들러.
     * @param {React.DragEvent} event - 드래그 이벤트.
     */
    const handleDragOver = (event) => {
        event.preventDefault();
        setIsDragging(true);
    };
    
    /**
     * 파일 드래그가 영역에서 나갔을 때 실행되는 핸들러.
     * @param {React.DragEvent} event - 드래그 이벤트.
     */
    const handleDragLeave = (event) => {
        event.preventDefault();
        setIsDragging(false);
    };
    
    /**
     * 파일 드롭 시 실행되는 핸들러.
     * @param {React.DragEvent} event - 드롭 이벤트.
     */
    const handleDrop = (event) => {
        event.preventDefault();
        setIsDragging(false);
        addFiles(event.dataTransfer.files);
    };
    
    /**
     * 사용자 목록에서 특정 사용자를 클릭했을 때 프로필 모달을 여는 핸들러.
     * @param {import('../context/ChatContext').ChatUser} clickedUser - 클릭된 사용자 정보.
     * @param {React.MouseEvent} event - 마우스 클릭 이벤트.
     */
    const handleUserClick = async (clickedUser, event) => {
        const liRect = event.currentTarget.getBoundingClientRect();
        const position = {
            top: liRect.top,
            left: liRect.left,
        };
        try {
            const response = await axiosInstance.get(`/user/${clickedUser.userId}/profile`);
            openUserProfileModal(response.data, position);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            toast.error('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };

    /**
     * 사용자 목록에서 우클릭 시 컨텍스트 메뉴를 표시하는 핸들러.
     * @param {React.MouseEvent} e - 마우스 우클릭 이벤트.
     * @param {import('../context/ChatContext').ChatUser} clickedUser - 우클릭된 사용자 정보.
     */
    const handleUserContextMenu = (e, clickedUser) => {
        e.preventDefault(); 

        if (clickedUser.userId === user.userId) {
            setContextMenu(null);
            return;
        }

        const menuItems = [];
        menuItems.push({
            label: `${clickedUser.nickname} 멘션하기`,
            action: () => handleMentionUser(clickedUser.nickname),
            isDanger: false,
        });

        if (myRole === 'ADMIN' && clickedUser.role === 'MEMBER') {
            menuItems.push({
                label: `${clickedUser.nickname} 추방`,
                action: () => handleKickUser(clickedUser.userId),
                isDanger: true,
            });
        }
        
        if (menuItems.length > 0) {
            setContextMenu({ x: e.clientX, y: e.clientY, user: clickedUser, items: menuItems });
        } else {
            setContextMenu(null);
        }
    };

    /**
     * 컨텍스트 메뉴 바깥쪽을 클릭하면 메뉴를 닫는 Effect.
     */
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (contextMenuRef.current && !contextMenuRef.current.contains(event.target)) {
                setContextMenu(null);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [contextMenu]);

    /**
     * 컨텍스트 메뉴에서 '추방'을 선택했을 때 실행되는 핸들러.
     * @param {number} userIdToKick - 추방할 사용자의 ID.
     */
    const handleKickUser = async (userIdToKick) => {
        setContextMenu(null);
        try {
            await axiosInstance.delete(`/api/rooms/${currentRoomId}/users/${userIdToKick}`);
            toast.success("사용자를 방에서 추방했습니다.");
        } catch (error) {
            console.error('Failed to kick user:', error);
            toast.error(error.response?.data?.message || '추방에 실패했습니다.');
        }
    };

    /**
     * 컨텍스트 메뉴에서 '멘션하기'를 선택했을 때 실행되는 핸들러.
     * @param {string} nickname - 멘션할 사용자의 닉네임.
     */
    const handleMentionUser = (nickname) => {
        setNewMessage(prev => {
            const currentText = prev.endsWith(' ') || prev.length === 0 ? prev : prev + ' ';
            return currentText + `@${nickname} `;
        });
        textareaRef.current?.focus();
        setContextMenu(null);
    };

    /**
     * 메시지 삭제 핸들러.
     * @param {number} messageId - 삭제할 메시지의 ID.
     */
    const handleDeleteMessage = async (messageId) => {
        if (window.confirm("이 메시지를 삭제하시겠습니까?")) {
            try {
                await axiosInstance.delete(`/api/messages/${messageId}`);
            } catch (error) {
                console.error('Failed to delete message:', error);
                toast.error(error.response?.data?.message || '메시지 삭제에 실패했습니다.');
            }
        }
    };

    /**
     * 메시지 수정 핸들러.
     * @param {number} messageId - 수정할 메시지의 ID.
     * @param {string} newContent - 새로운 메시지 내용.
     */
    const handleEditMessage = async (messageId, newContent) => {
        try {
            await axiosInstance.put(`/api/messages/${messageId}`, { content: newContent });
        } catch (error) {
            console.error('Failed to edit message:', error);
            toast.error(error.response?.data?.message || '메시지 수정에 실패했습니다.');
        }
    };
    
    /**
     * 파일 미리보기 목록에서 특정 파일을 제거하는 핸들러.
     * @param {File} fileToRemove - 제거할 파일 객체.
     */
    const handleRemoveFile = (fileToRemove) => {
        setFilesToUpload(prevFiles => prevFiles.filter(item => item.file !== fileToRemove));
    };
    
    /**
     * 클립보드 붙여넣기 이벤트를 감지하여 이미지 파일을 첨부하는 Effect.
     */
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
                                    onClick={(event) => handleUserClick(u, event)}
                                    onContextMenu={(event) => handleUserContextMenu(event, u)}>
                                    <img src={`${SERVER_URL}${u.profileImageUrl}`} alt={u.nickname} className="user-list-profile-img" />
                                    <span className="user-list-nickname">{u.nickname}</span>
                                </li>
                            ))}
                        </ul>
                        {contextMenu && (
                            <div
                                ref={contextMenuRef}
                                className="custom-context-menu"
                                style={{ top: contextMenu.y, left: contextMenu.x }}
                            >
                                {contextMenu.items.map((item, index) => (
                                    <div
                                        key={index}
                                        className={`context-menu-item ${item.isDanger ? 'danger' : ''}`}
                                        onClick={item.action}
                                    >
                                        {item.label}
                                    </div>
                                ))}
                            </div>
                        )}
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
                                {messages.map((msg, index) => {
                                    const prevMsg = messages[index - 1];
                                    const isFirstInGroup = !prevMsg || 
                                                           prevMsg.authorId !== msg.authorId || 
                                                           (new Date(msg.createdAt) - new Date(prevMsg.createdAt)) > 120000;
                                    
                                    return <ChatMessage 
                                                key={msg.messageId || `msg-${index}`} 
                                                message={msg} 
                                                isFirstInGroup={isFirstInGroup}
                                                myRole={myRole}
                                                handleDeleteMessage={handleDeleteMessage}
                                                handleEditMessage={handleEditMessage}
                                            />;
                                })}
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