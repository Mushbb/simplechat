import React, { useState, useContext, useRef, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaBell, FaUserFriends } from 'react-icons/fa';
import FriendListModal from './FriendListModal';

function Topbar() {
    const { user, logout, deleteAccount, openLoginModal, openRegisterModal, openProfileModal,
        notifications, acceptFriendRequest, rejectFriendRequest, openFriendListModal, isFriendListModalOpen } = useContext(AuthContext);
    const { joinedRooms, activeRoomId, setActiveRoomId, exitRoom, deleteRoom, usersByRoom, unreadRooms} = useContext(ChatContext);
    const navigate = useNavigate();
    const location = useLocation();
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const dropdownRef = useRef(null);
    
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
    
    return (
        <header className="topbar">
            <div className="topbar-main">
                <div className="topbar-auth-controls">
                    {user ? (
                        <>
                            <div className="topbar-icon-container">
                                <button className="topbar-icon-btn" onClick={openFriendListModal}>
                                    <FaUserFriends />
                                </button>
                                {isFriendListModalOpen && <FriendListModal />}
                            </div>
                            <div className="topbar-icon-container" ref={dropdownRef}>
                                <button className="topbar-icon-btn notification-bell" onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
                                    <FaBell />
                                    {notifications.length > 0 && <span className="notification-badge">{notifications.length}</span>}
                                </button>
                                {isDropdownOpen && (
                                    <div className="notification-dropdown">
                                        {notifications.length > 0 ? (
                                            notifications.map(n => (
                                                <div key={n.userId} className="notification-item">
                                                    <span>{n.nickname}님이 친구 요청을 보냈습니다.</span>
                                                    <div className="notification-actions">
                                                        <button onClick={() => acceptFriendRequest(n.userId)}>수락</button>
                                                        <button className="danger-button" onClick={() => rejectFriendRequest(n.userId)}>거절</button>
                                                    </div>
                                                </div>
                                            ))
                                        ) : (
                                            <div className="notification-item">새로운 알림이 없습니다.</div>
                                        )}
                                    </div>
                                )}
                            </div>
                            <span>{user.nickname}님</span>
                            <button onClick={openProfileModal}>프로필 수정</button>
                            <button onClick={handleLogout}>로그아웃</button>
                            <button onClick={deleteAccount} className="danger-button">회원 탈퇴</button>
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