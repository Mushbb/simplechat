import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ChatContext } from '../context/ChatContext';
import { Link, useNavigate, useLocation } from 'react-router-dom';

function Topbar() {
  // 1. AuthContext에서 필요한 값들을 가져옵니다.
    const { user, logout, deleteAccount, openLoginModal, openRegisterModal, openProfileModal } = useContext(AuthContext);
    const { joinedRooms, activeRoomId, setActiveRoomId } = useContext(ChatContext);
    const navigate = useNavigate();
    const location = useLocation();

    const handleTabClick = (roomId) => {
        setActiveRoomId(roomId); // Context에 현재 활성화된 방이 무엇인지 알립니다.
        navigate(`/chat/${roomId}`); // 해당 방의 URL로 페이지를 이동시킵니다.
    };
    
    // ✅ 새로운 로그아웃 핸들러 함수를 만듭니다.
    const handleLogout = async () => {
        await logout(); // 기존의 logout 함수를 호출해서 상태를 변경하고
        navigate('/');  // 작업이 끝나면 로비로 이동시킵니다.
    };
    
  return (
    <header className="topbar">
        <div className="topbar-auth-controls">
            {user ? (
                <>
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
        {user && (
            <nav className="room-tabs-container">
                <button
                    className={`room-tab ${location.pathname === '/' ? 'active' : ''}`}
                    onClick={() => navigate('/')}
                >
                    로비
                </button>
                {joinedRooms.map(room => (
                    <button
                        key={room.id}
                        className={`room-tab ${room.id === activeRoomId ? 'active' : ''}`}
                        onClick={() => handleTabClick(room.id)}
                    >
                        {room.name}
                    </button>
                ))}
            </nav>
        )}
    </header>
  );
}

export default Topbar;
