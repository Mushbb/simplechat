import React, { useState, useContext, useRef, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import axiosInstance from '../api/axiosInstance';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaBell, FaUserFriends } from 'react-icons/fa';
import FriendListModal from './FriendListModal';

function Topbar() {
    const { user, logout, deleteAccount, openLoginModal, openRegisterModal, openProfileModal,
        notifications, acceptNotification, rejectNotification, toggleFriendListModal, friendModalConfig,
        openUserProfileModal } = useContext(AuthContext);
    const { joinedRooms, activeRoomId, setActiveRoomId, exitRoom, deleteRoom, usersByRoom, unreadRooms} = useContext(ChatContext);
    const navigate = useNavigate();
    const location = useLocation();
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    
    const dropdownRef = useRef(null);
    const friendIconRef = useRef(null);
    
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [dropdownRef]);
    
    const isActiveRoomChat = location.pathname.startsWith('/chat/') && activeRoomId;
    const usersInActiveRoom = usersByRoom[activeRoomId] || [];
    const myRoleInActiveRoom = usersInActiveRoom.find(u => u.userId === user?.userId)?.role;
    
    const handleProfileClick = async (friend, event) => {
        const liRect = event.currentTarget.getBoundingClientRect();
        const position = { top: liRect.top, left: liRect.right + 5 };
        
        try {
            // 전체 프로필 정보를 API로 가져옵니다 (상세 정보 포함).
            const response = await axiosInstance.get(`/user/${friend.userId}/profile`);
            // AuthContext의 전역 함수를 호출하여 프로필 모달을 엽니다.
            openUserProfileModal(response.data, position);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            alert('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };
    
    const handleOpenFriendList = () => {
        // 아이콘 버튼의 위치 계산
        const rect = friendIconRef.current.getBoundingClientRect();
        
        // 👈 변경: openFriendListModal 호출 시 위치 정보 전달
        toggleFriendListModal({
            title: '친구 목록',
            onFriendClick: handleProfileClick, // 기존 프로필 클릭 로직
            position: {
                top: rect.bottom, // 아이콘 바로 아래 5px 지점
                left: rect.left - 90     // 아이콘 왼쪽 끝에 맞춤
            }
        });
    };
    
    const handleTabClick = (roomId) => {
        setActiveRoomId(roomId); // Context에 현재 활성화된 방이 무엇인지 알립니다.
        navigate(`/chat/${roomId}`); // 해당 방의 URL로 페이지를 이동시킵니다.
    };
    
    // ✅ 새로운 로그아웃 핸들러 함수를 만듭니다.
    const handleLogout = async () => {
        await logout(); // 기존의 logout 함수를 호출해서 상태를 변경하고
        navigate('/');  // 작업이 끝나면 로비로 이동시킵니다.
    };
    
    // ✅ 4. 방 나가기 핸들러
    const handleExitRoom = () => {
        if (window.confirm("정말로 이 방에서 나가시겠습니까?")) {
            exitRoom(activeRoomId);
            navigate('/'); // 로비로 이동
        }
    };
    
    // ✅ 5. 방 삭제 핸들러
    const handleDeleteRoom = () => {
        if (window.confirm("정말로 이 방을 삭제하시겠습니까? 모든 대화 내용이 사라집니다.")) {
            deleteRoom(activeRoomId);
            navigate('/'); // 로비로 이동
        }
    };
    
    // ✨ 신규: 수락 버튼 클릭 시 실행될 새로운 핸들러
    const handleAcceptNotification = async (notification) => {
        const roomId = await acceptNotification(notification);
        // 만약 acceptNotification 함수가 roomId를 반환했다면
        if (roomId) {
            // 해당 채팅방 URL로 페이지를 이동시킵니다.
            navigate(`/chat/${roomId}`);
        }
    };
    
    return (
        <header className="topbar">
            <div className="topbar-main">
                <div className="topbar-auth-controls">
                    {user ? (
                        <>
                            <div className="topbar-icon-container">
                                <button ref={friendIconRef}
                                        className="topbar-icon-btn"
                                        onClick={handleOpenFriendList}
                                        data-modal-toggle="friendlist"
                                >
                                    <FaUserFriends />
                                </button>
                                {friendModalConfig.isOpen && <FriendListModal />}
                            </div>
                            <div className="topbar-icon-container" ref={dropdownRef}>
                                <button className="topbar-icon-btn notification-bell" onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
                                    <FaBell />
                                    {notifications.length > 0 && <span className="notification-badge">{notifications.length}</span>}
                                </button>
                                {isDropdownOpen && (
                                    <div className="notification-dropdown">
                                        {notifications.length > 0 ? (
                                            // 👈 변경: 새로운 notifications 배열을 렌더링
                                            notifications.map(n => (
                                                // 👈 변경: JSX 구조를 텍스트와 버튼 영역으로 매우 단순하게 변경합니다.
                                                <div key={n.notificationId} className="notification-item">
                                                    <span className="notification-text">{n.content}</span>
                                                    <div className="notification-actions">
                                                        <button onClick={() => acceptNotification(n)}>수락</button>
                                                        <button className="danger-button" onClick={() => rejectNotification(n.notificationId)}>거절</button>
                                                    </div>
                                                </div>
                                            ))
                                        ) : (
                                            <div className="notification-item">새로운 알림이 없습니다.</div>
                                        )}
                                    </div>
                                )}
                            </div>
                            <div className="auth-controls-btn">
                                <span>{user.nickname}님</span>
                                <button onClick={openProfileModal}>프로필 수정</button>
                                <button onClick={handleLogout}>로그아웃</button>
                                <button onClick={deleteAccount} className="danger-button">회원 탈퇴</button>
                            </div>
                        </>
                    ) : (
                        <>
                            <button onClick={openLoginModal}>로그인</button>
                            <button onClick={openRegisterModal}>회원가입</button>
                        </>
                    )}
                </div>
                {isActiveRoomChat && (
                    <div className="room-actions">
                        {/* 내가 방장이 아닐 때만 '방 나가기' 버튼이 보입니다. */}
                        {myRoleInActiveRoom !== 'ADMIN' && (
                            <button onClick={handleExitRoom}>방 나가기</button>
                        )}
                        {/* 내가 방장(ADMIN)일 때만 방 삭제 버튼이 보입니다. */}
                        {myRoleInActiveRoom === 'ADMIN' && (
                            <button onClick={handleDeleteRoom} className="danger-button">
                                방 삭제
                            </button>
                        )}
                    </div>
                )}
            </div>
            {user && (
                <nav className="room-tabs-container">
                    <button
                        className={`room-tab ${location.pathname === '/' ? 'active' : ''}`}
                        onClick={() => navigate('/')}
                    >
                        로비
                    </button>
                    {joinedRooms.map(room => {
                        // 이 방이 안 읽은 메시지를 가지고 있다면
                        const hasUnread = unreadRooms.has(room.id);
                        
                        return (
                            <button
                                key={room.id}
                                // hasUnread가 true일 때 'unread' 클래스를 추가
                                className={`room-tab ${room.id === activeRoomId ? 'active' : ''} ${hasUnread ? 'unread' : ''}`}
                                onClick={() => handleTabClick(room.id)}
                            >
                                {room.name}
                            </button>
                        );
                    })}
                </nav>
            )}
        </header>
    );
}

export default Topbar;