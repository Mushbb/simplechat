import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';

/**
 * @file 사용자 로그인을 위한 모달 컴포넌트입니다.
 */

/**
 * 로그인 폼을 포함하는 모달 컴포넌트.
 * 사용자는 이 모달을 통해 아이디와 비밀번호를 입력하여 로그인할 수 있습니다.
 * 회원가입 모달로 전환하는 기능도 포함합니다.
 * @returns {JSX.Element} AuthModal 컴포넌트의 JSX.
 */
function AuthModal() {
    const { login } = useContext(AuthContext);
    const { closeLoginModal, openRegisterModal } = useContext(ModalContext);

    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 사용자 아이디 입력 상태 */
    const [username, setUsername] = useState('');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 사용자 비밀번호 입력 상태 */
    const [password, setPassword] = useState('');

    /**
     * 폼 제출 시 로그인 로직을 실행하는 핸들러.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            await login(username, password);
            closeLoginModal(); // 로그인 성공 후 모달 닫기
        } catch (error) {
            // 에러 처리는 login 함수 내부에서 이미 처리됨 (alert)
        }
    };

    /**
     * '샘플 계정으로 로그인' 버튼 클릭 시 실행되는 핸들러.
     */
    const handleSampleLogin = async () => {
        try {
            await login('sample', 'sample');
            closeLoginModal(); // 로그인 성공 후 모달 닫기
        } catch (error) {
            // 에러 처리는 login 함수 내부에서 이미 처리됨 (alert)
        }
    };
    
    /**
     * 회원가입 모달로 전환하는 핸들러.
     */
    const handleSwitchToRegister = () => {
        closeLoginModal();
        openRegisterModal();
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
                <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                    <button type="button" className="modal-submit-btn" onClick={handleSampleLogin} style={{ flex: 2, backgroundColor: '#6c757d' }}>
                        샘플 계정으로 로그인
                    </button>
                    <button type="button" className="modal-submit-btn" onClick={handleSwitchToRegister} style={{ flex: 1, backgroundColor: '#28a745' }}>
                        회원가입
                    </button>
                </div>
            </div>
        </div>
    );
}

export default AuthModal;
