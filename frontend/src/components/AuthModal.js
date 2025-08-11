import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

// 간단한 모달 스타일
const modalOverlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100%',
  height: '100%',
  backgroundColor: 'rgba(0, 0, 0, 0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
};

const modalContentStyle = {
  backgroundColor: 'white',
  padding: '20px 40px',
  borderRadius: '8px',
  boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
};

function AuthModal() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useContext(AuthContext); // Context에서 login 함수를 가져옵니다.

  const handleSubmit = (e) => {
    e.preventDefault(); // 폼 제출 시 페이지가 새로고침되는 것을 방지
    if (!username || !password) {
        alert('사용자 이름과 비밀번호를 모두 입력하세요.');
        return;
    }
    login(username, password);
    // 성공 시 모달을 닫는 로직은 App.js에서 처리할 예정입니다.
  };

  return (
    <div style={modalOverlayStyle}>
      <div style={modalContentStyle}>
        <h2>로그인</h2>
        <form onSubmit={handleSubmit}>
          <div style={{ marginTop: '15px' }}>
            <label htmlFor="username" style={{ display: 'block', marginBottom: '5px' }}>사용자 이름</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={{ padding: '8px', width: '200px' }}
            />
          </div>
          <div style={{ marginTop: '15px' }}>
            <label htmlFor="password" style={{ display: 'block', marginBottom: '5px' }}>비밀번호</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ padding: '8px', width: '200px' }}
            />
          </div>
          <button type="submit" style={{ marginTop: '20px', padding: '10px 15px', width: '100%' }}>
            로그인
          </button>
        </form>
      </div>
    </div>
  );
}

export default AuthModal;
