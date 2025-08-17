import React, { useEffect, useContext, useState, useRef, useLayoutEffect } from 'react';
import { useParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import ChatMessage from './ChatMessage';
import UserProfileModal from './UserProfileModal';
import { IoSend } from "react-icons/io5";
import axiosInstance from '../api/axiosInstance';

const SERVER_URL = 'http://localhost:8080';

function ChatPage() {
    const { roomId } = useParams();
    const { user } = useContext(AuthContext);
    const { setActiveRoomId, messagesByRoom, usersByRoom, joinedRooms, stompClientsRef } = useContext(ChatContext);

    // --- UI 상호작용을 위한 Local State ---
    const [newMessage, setNewMessage] = useState('');
    const [myNickname, setMyNickname] = useState('');
    const [filesToUpload, setFilesToUpload] = useState([]);
    const [isUploading, setIsUploading] = useState(false);
    const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
    const [selectedProfile, setSelectedProfile] = useState(null);
    const [modalPosition, setModalPosition] = useState({ top: 0, left: 0 });

    // --- DOM 참조 및 스크롤 관리를 위한 Ref ---
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);
    const scrollContainerRef = useRef(null);
    const prevScrollHeightRef = useRef(null);

    const currentRoomId = Number(roomId);
    const roomName = joinedRooms.find(r => r.id === currentRoomId)?.name || '';
    const messages = messagesByRoom[currentRoomId] || [];
    const users = usersByRoom[currentRoomId] || [];
    const scrollActionRef = useRef('initial');

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

    useLayoutEffect(() => {
        const container = scrollContainerRef.current;
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }, [messages]);

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
            handleSendMessage(e);
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
        const selectedFiles = Array.from(event.target.files);
        if (selectedFiles.length === 0) return;
        selectedFiles.sort((a, b) => a.name.localeCompare(b.name));
        const filePromises = selectedFiles.map(file => new Promise((resolve) => {
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
        });
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
                alert(`${item.file.name} 업로드에 실패했습니다.`);
                break;
            }
        }
        setIsUploading(false);
        setFilesToUpload([]);
    };

    const handleUserClick = async (clickedUserId, event) => { /* ... 이전과 동일 ... */ };
    const handleRemoveFile = (fileToRemove) => { /* ... 이전과 동일 ... */ };

    return (
        <div className="chat-page-container">
            <div data-id="chat-main-flex-container" className="chat-main-flex-container">
                {isProfileModalOpen && (
                    <UserProfileModal profile={selectedProfile} onClose={() => setIsProfileModalOpen(false)} position={modalPosition} />
                )}
                <div data-id="user-list-panel" className="user-list-panel">
                    <h2 className="panel-title">{roomName}</h2>
                    <h4>멤버 목록 ({users.filter(u => u.conn === 'CONNECT').length} / {users.length})</h4>
                    <ul className="user-list-scrollable">
                        {users.map(u => (
                            <li key={u.userId} className={`user-list-item ${u.userId === user.userId ? 'me' : ''} ${u.conn === 'DISCONNECT' ? 'disconnected' : ''}`}
                                onClick={(event) => handleUserClick(u.userId, event)}>
                                <img src={`${SERVER_URL}${u.profileImageUrl}`} alt={u.nickname} className="user-list-profile-img" />
                                <span className="user-list-nickname">{u.nickname}</span>
                            </li>
                        ))}
                    </ul>
                    <div className="nickname-editor">
                        <input type="text" value={myNickname} onChange={(e) => setMyNickname(e.target.value)} onBlur={handleNicknameUpdate}
                               onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleNicknameUpdate(); e.target.blur(); }}}/>
                    </div>
                </div>
                <div className="chat-panel">
                    <div ref={scrollContainerRef} className="chat-message-list">
                        {messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)}
                    </div>
                    {filesToUpload.length > 0 && (
                        <div className="file-preview-container">
                            <div className="file-preview-list">{/* ... */}</div>
                            <div className="file-preview-actions">{/* ... */}</div>
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