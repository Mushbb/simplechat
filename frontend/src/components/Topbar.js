import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { Link } from 'react-router-dom';

function Topbar() {
  // 1. AuthContext에서 필요한 값들을 가져옵니다.
    const { user, logout, deleteAccount, openLoginModal, openRegisterModal, openProfileModal } = useContext(AuthContext);

  return (
    <header className="topbar">
        <h1><Link to="/" className="topbar-logo">Simple Chat</Link></h1>
        <div className="topbar-auth-controls">
            {user ? (
                <>
                    <span>{user.nickname}님</span>
                    <button onClick={openProfileModal}>프로필 수정</button>
                    <button onClick={logout}>로그아웃</button>
                    <button onClick={deleteAccount} className="danger-button">회원 탈퇴</button>
                </>
            ) : (
                <>
                    <button onClick={openLoginModal}>로그인</button>
                    <button onClick={openRegisterModal}>회원가입</button>
                </>
            )}
        </div>
    </header>
  );
}

export default Topbar;
