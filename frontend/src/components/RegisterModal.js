import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext'; // AuthContext를 가져옵니다.

// AuthModal.js와 유사한 스타일을 사용합니다.
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
    zIndex: 1000,
};

const modalContentStyle = {
    backgroundColor: 'white',
    padding: '20px 40px',
    borderRadius: '8px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
};


function RegisterModal() {
    const [username, setUsername] = useState('');
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');

    // ✅ Context에서 register 함수와 closeModal 함수를 가져옵니다.
    const { register, closeRegisterModal } = useContext(AuthContext);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!username || !nickname || !password) {
            alert('모든 필드를 입력해주세요.');
            return;
        }
        register(username, nickname, password);
    };

    return (
        <div className="modal-overlay" onClick={closeRegisterModal}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close-btn" onClick={closeRegisterModal}>&times;</button>
                <h2>회원가입</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="reg-username">아이디</label>
                        <input
                            type="text"
                            id="reg-username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-nickname">닉네임</label>
                        <input
                            type="text"
                            id="reg-nickname"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="reg-password">비밀번호</label>
                        <input
                            type="password"
                            id="reg-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <button type="submit" className="modal-submit-btn">가입하기</button>
                </form>
            </div>
        </div>
    );
}

export default RegisterModal;