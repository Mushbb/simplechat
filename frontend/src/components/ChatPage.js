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
    const { user } = useContext(AuthContext);
    const { setActiveRoomId, messagesByRoom, usersByRoom, joinedRooms, stompClientsRef, isRoomLoading } = useContext(ChatContext);

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
    const isLoading = isRoomLoading[currentRoomId] !== false;
    
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
        });
    }, []); // 의존성 배열이 비어있어도 괜찮습니다. (setFilesToUpload는 항상 동일)
    
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
                        {/* ✅ 3. 로딩 상태에 따라 조건부 렌더링 */}
                        {isLoading ? (
                            <div style={{ textAlign: 'center', padding: '20px' }}>
                                채팅 내역을 불러오는 중입니다...
                            </div>
                        ) : (
                            messages.map((msg, index) => <ChatMessage key={msg.messageId || `msg-${index}`} message={msg} />)
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