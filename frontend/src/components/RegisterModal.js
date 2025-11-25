import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { ModalContext } from '../context/ModalContext';

/**
 * @file 사용자 회원가입을 위한 모달 컴포넌트입니다.
 */

/**
 * 회원가입 폼을 포함하는 모달 컴포넌트.
 * 사용자는 아이디, 닉네임, 비밀번호를 입력하여 새로운 계정을 생성할 수 있습니다.
 * @returns {JSX.Element} RegisterModal 컴포넌트의 JSX.
 */
function RegisterModal() {
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 아이디 입력 상태 */
    const [username, setUsername] = useState('');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 닉네임 입력 상태 */
    const [nickname, setNickname] = useState('');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 비밀번호 입력 상태 */
    const [password, setPassword] = useState('');
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 비밀번호 확인 입력 상태 */
    const [passwordConfirm, setPasswordConfirm] = useState('');

    const { register } = useContext(AuthContext);
    const { closeRegisterModal } = useContext(ModalContext);

    /**
     * 폼 제출 시 회원가입 로직을 실행하는 핸들러.
     * 비밀번호 일치 여부를 확인한 후, `register` 컨텍스트 함수를 호출합니다.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
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
            // 에러 처리는 register 함수 내부에서 이미 처리됨 (alert)
        }
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && closeRegisterModal()}>
            <div className="modal-content">
                <button className="modal-close-btn" onClick={closeRegisterModal}>&times;</button>
                <h2>회원가입</h2>
                <form onSubmit={handleRegister}>
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