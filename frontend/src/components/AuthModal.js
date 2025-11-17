import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';

function AuthModal() {
    const { login } = useContext(AuthContext);
    const { closeLoginModal } = useContext(ModalContext);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            await login(username, password);
            closeLoginModal(); // 로그인 성공 후 모달 닫기
        } catch (error) {
            // 에러 처리는 login 함수 내부에서 이미 처리됨
        }
    };

    const handleSampleLogin = async () => {
        try {
            await login('sample', 'sample');
            closeLoginModal(); // 로그인 성공 후 모달 닫기
        } catch (error) {
            // 에러 처리는 login 함수 내부에서 이미 처리됨
        }
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && closeLoginModal()}>
            <div className="modal-content">
                <button className="modal-close-btn" onClick={closeLoginModal}>&times;</button>
                <h2>로그인</h2>
                <form onSubmit={handleLogin}>
                    <div className="form-group">
                        <label htmlFor="username">아이디</label>
                        <input
                            type="text"
                            id="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            autoFocus
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
                    <button type="submit" className="modal-submit-btn">로그인</button>
                </form>
                <button type="button" className="modal-submit-btn" onClick={handleSampleLogin} style={{marginTop: '10px', backgroundColor: '#6c757d'}}>
                    샘플 계정으로 로그인
                </button>
            </div>
        </div>
    );
}

export default AuthModal;
