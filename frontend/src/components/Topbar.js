import React, { useState, useContext, useRef, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import { NotificationContext } from '../context/NotificationContext';
import { FriendContext } from '../context/FriendContext';
import { ModalContext } from '../context/ModalContext';
import { RoomContext } from '../context/RoomContext';
import axiosInstance from '../api/axiosInstance';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaBell, FaUserFriends } from 'react-icons/fa';
import { toast } from 'react-toastify';
import FriendListModal from './FriendListModal';
import '../styles/Notifications.css';
import '../styles/Topbar.css';

/**
 * @file 애플리케이션의 최상단에 위치하는 Topbar 컴포넌트입니다.
 * 인증, 알림, 친구 목록, 채팅방 탭 등 전역적인 탐색 및 상호작용 기능을 제공합니다.
 */

/**
 * Topbar 컴포넌트.
 * @returns {JSX.Element} Topbar 컴포넌트의 JSX.
 */
function Topbar() {
    const { user, logout, deleteAccount, isAdmin } = useContext(AuthContext);
    const { notifications, unreadCount, acceptNotification, rejectNotification, markNotificationsAsRead } = useContext(NotificationContext);
    const { openLoginModal, openRegisterModal, openProfileModal, toggleFriendListModal, openUserProfileModal, friendModalConfig } = useContext(ModalContext);
    const { joinedRooms, activeRoomId, setActiveRoomId, unreadRooms } = useContext(RoomContext);
    const navigate = useNavigate();
    const location = useLocation();

    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 알림 드롭다운 메뉴의 열림/닫힘 상태 */
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 관리자 명령 입력창의 상태 */
    const [adminCommand, setAdminCommand] = useState('');
    
    /** @type {React.RefObject<HTMLDivElement>} 알림 드롭다운 DOM 엘리먼트에 대한 Ref */
    const dropdownRef = useRef(null);
    /** @type {React.RefObject<HTMLButtonElement>} 친구 목록 아이콘 버튼 DOM 엘리먼트에 대한 Ref */
    const friendIconRef = useRef(null);

    /**
     * 관리자 명령 폼 제출 시 실행되는 핸들러.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
    const handleAdminCommand = async (e) => {
        e.preventDefault();
        if (!adminCommand.trim()) return;

        try {
            const response = await axiosInstance.post('/api/admin/command', { command: adminCommand });
            toast.success(`명령 실행 성공: ${response.data.message}`);
            setAdminCommand('');
        } catch (error) {
            console.error('Admin command failed:', error);
            toast.error(error.response?.data?.message || '명령 실행에 실패했습니다.');
        }
    };
    
    /**
     * 알림 드롭다운 외부 클릭 시 드롭다운을 닫는 Effect.
     */
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [dropdownRef]);
    
    /**
     * 친구 목록에서 친구 클릭 시 프로필 모달을 여는 핸들러.
     * @param {import('../context/FriendContext').Friend} friend - 클릭된 친구 객체.
     * @param {React.MouseEvent} event - 마우스 클릭 이벤트.
     */
    const handleProfileClick = async (friend, event) => {
        const liRect = event.currentTarget.getBoundingClientRect();
        const position = { top: liRect.top, left: liRect.right + 5 };
        
        try {
            const response = await axiosInstance.get(`/user/${friend.userId}/profile`);
            openUserProfileModal(response.data, position);
        } catch (error) {
            console.error('프로필 정보를 가져오는 데 실패했습니다:', error);
            alert('프로필 정보를 가져오는 데 실패했습니다.');
        }
    };
    
    /**
     * 친구 아이콘 클릭 시 친구 목록 모달을 토글하는 핸들러.
     * 모달이 열릴 위치를 동적으로 계산하여 전달합니다.
     */
    const handleOpenFriendList = () => {
        const rect = friendIconRef.current.getBoundingClientRect();
        toggleFriendListModal({
            title: '친구 목록',
            onFriendClick: handleProfileClick,
            position: {
                mode: 'absolute',
                top: rect.bottom - 10,
                left: rect.left - 90
            }
        });
    };
    
    /**
     * 채팅방 탭 클릭 시 해당 채팅방으로 이동하는 핸들러.
     * @param {number} roomId - 이동할 채팅방의 ID.
     */
    const handleTabClick = (roomId) => {
        setActiveRoomId(roomId);
        navigate(`/chat/${roomId}`);
    };
    
    /**
     * 로그아웃 버튼 클릭 시 로그아웃을 처리하고 홈페이지로 이동하는 핸들러.
     */
    const handleLogout = async () => {
        await logout();
        navigate('/');
    };
    
    /**
     * 알림 수락 시 후속 조치를 처리하는 핸들러.
     * 방 초대 알림인 경우, 수락 후 해당 채팅방으로 이동합니다.
     * @param {import('../context/NotificationContext').Notification} notification - 수락된 알림 객체.
     */
    const handleAcceptNotification = async (notification) => {
        const roomId = await acceptNotification(notification);
        if (roomId) {
            navigate(`/chat/${roomId}`);
        }
    };

    /**
     * 알림 벨 아이콘 클릭 시 드롭다운을 토글하고, 열릴 때 읽지 않은 알림을 읽음 처리하는 핸들러.
     */
    const handleBellClick = () => {
        setIsDropdownOpen(prev => {
            const newState = !prev;
            if (newState && notifications.length > 0) {
                const notificationIdsToMarkAsRead = notifications
                    .filter(n => !n.isRead)
                    .map(n => n.notificationId);
                if (notificationIdsToMarkAsRead.length > 0) {
                    markNotificationsAsRead(notificationIdsToMarkAsRead);
                }
            }
            return newState;
        });
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
                                <button className="topbar-icon-btn notification-bell" onClick={handleBellClick}>
                                    <FaBell />
                                    {unreadCount > 0 && <span className="notification-badge">{unreadCount}</span>}
                                </button>
                                {isDropdownOpen && (
                                    <div className="notification-dropdown">
                                        {notifications.length > 0 ? (
                                            notifications.map(n => (
                                                <div key={n.notificationId} className={`notification-item ${n.isRead ? 'read' : ''}`}>
                                                    <span className="notification-text">{n.content}</span>
                                                    {n.type !== 'MENTION' && (
                                                        <div className="notification-actions">
                                                            <button onClick={() => handleAcceptNotification(n)}>수락</button>
                                                            <button className="danger-button" onClick={() => rejectNotification(n.notificationId)}>거절</button>
                                                        </div>
                                                    )}
                                                </div>
                                            ))
                                        ) : (
                                            <div className="notification-item">새로운 알림이 없습니다.</div>
                                        )}
                                    </div>
                                )}
                            </div>
                            <div className="auth-controls-btn">
                                <span>{user.nickname}님 </span>
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
            </div>
            {isAdmin && (
                <div className="topbar-admin-bar">
                    <form onSubmit={handleAdminCommand} style={{ display: 'flex', alignItems: 'center', padding: '5px 10px', backgroundColor: '#333' }}>
                        <input
                            type="text"
                            value={adminCommand}
                            onChange={(e) => setAdminCommand(e.target.value)}
                            placeholder="Enter admin command..."
                            style={{ flex: 1, marginRight: '10px' }}
                        />
                        <button type="submit">Execute</button>
                    </form>
                </div>
            )}
            {user && (
                <nav className="room-tabs-container">
                    <button
                        className={`room-tab ${location.pathname === '/' ? 'active' : ''}`}
                        onClick={() => navigate('/')}
                    >
                        로비
                    </button>
                    {joinedRooms.map(room => {
                        const hasUnread = unreadRooms.has(room.id);
                        
                        return (
                            <button
                                key={room.id}
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