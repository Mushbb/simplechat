import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';

function RegisterModal() {
    const [username, setUsername] = useState('');
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState(''); // passwordConfirm 상태 추가

    const { register } = useContext(AuthContext);
    const { closeRegisterModal } = useContext(ModalContext);

    const handleRegister = async (e) => {
        e.preventDefault();
        if (password !== passwordConfirm) {
            alert("비밀번호가 일치하지 않습니다.");
            return;
        }
        try {
            await register(username, nickname, password);
            closeRegisterModal(); // 회원가입 성공 후 모달 닫기
        } catch (error) {
            // 에러 처리는 register 함수 내부에서 이미 처리됨
        }
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && closeRegisterModal()}>
            <div className="modal-content">
                <button className="modal-close-btn" onClick={closeRegisterModal}>&times;</button>
                <h2>회원가입</h2>
                <form onSubmit={handleRegister}> {/* onSubmit 핸들러를 handleRegister로 변경 */}
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
                    <div className="form-group">
                        <label htmlFor="passwordConfirm">비밀번호 확인</label>
                        <input
                            type="password"
                            id="passwordConfirm"
                            value={passwordConfirm}
                            onChange={(e) => setPasswordConfirm(e.target.value)}
                        />
                    </div>
                    <button type="submit" className="modal-submit-btn">가입하기</button>
                </form>
            </div>
        </div>
    );
}

export default RegisterModal;