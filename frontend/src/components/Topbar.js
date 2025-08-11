import React, { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

function Topbar() {
  // 1. AuthContext에서 필요한 값들을 가져옵니다.
  const { user, logout, openModal } = useContext(AuthContext);

  return (
    <div style={{
      backgroundColor: '#20232a',
      padding: '10px 20px',
      color: 'white',
      borderBottom: '1px solid #444',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center'
    }}>
      <h1>Simple Chat</h1>
      <div>
        {/* 2. user 상태에 따라 조건부 렌더링 */}
        {user ? (
          <>
            <span style={{ marginRight: '15px' }}>{user.nickname}님, 환영합니다!</span>
            <button onClick={logout}>로그아웃</button>
          </>
        ) : (
          <button onClick={openModal}>로그인</button>
        )}
      </div>
    </div>
  );
}

export default Topbar;
