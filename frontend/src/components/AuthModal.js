import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { toast } from 'react-toastify';

function AuthModal() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login, closeLoginModal } = useContext(AuthContext);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!username || !password) {
        toast.warn('사용자 이름과 비밀번호를 모두 입력하세요.');
        return;
    }
    login(username, password);
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      closeLoginModal();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content">
        <button className="modal-close-btn" onClick={closeLoginModal}>&times;</button>
        <h2>로그인</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">사용자 이름</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">비밀번호</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="modal-submit-btn">
            로그인
          </button>
        </form>
      </div>
    </div>
  );
}

export default AuthModal;
