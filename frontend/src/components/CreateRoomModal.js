import React, { useState } from 'react';
import { toast } from 'react-toastify';
import '../styles/Modals.css';

/**
 * @file 새로운 채팅방을 생성하기 위한 모달 컴포넌트입니다.
 */

/**
 * 새로운 채팅방 생성을 위한 폼을 담고 있는 모달 컴포넌트.
 * @param {object} props
 * @param {Function} props.onClose - 모달을 닫을 때 호출되는 함수.
 * @param {Function} props.onCreate - '만들기' 버튼 클릭 시 방 생성 데이터를 전달하며 호출되는 함수.
 * @returns {JSX.Element} CreateRoomModal 컴포넌트의 JSX.
 */
function CreateRoomModal({ onClose, onCreate }) {
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 생성할 방의 이름 상태 */
    const [roomName, setRoomName] = useState('');
    /** @type {[boolean, React.Dispatch<React.SetStateAction<boolean>>]} 방의 공개/비공개 여부 상태 */
    const [isPrivate, setIsPrivate] = useState(false);
    /** @type {[string, React.Dispatch<React.SetStateAction<string>>]} 비공개 방의 비밀번호 상태 */
    const [password, setPassword] = useState('');

    /**
     * 폼 제출 시 입력 값을 검증하고 `onCreate` 콜백을 호출하는 핸들러.
     * @param {React.FormEvent} e - 폼 제출 이벤트.
     */
    const handleSubmit = (e) => {
        e.preventDefault();
        if (!roomName.trim()) {
            toast.warn('방 이름을 입력해주세요.');
            return;
        }
        onCreate({ roomName, isPrivate, password });
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="modal-content">
                <button className="modal-close-btn" onClick={onClose}>&times;</button>
                <h2>새로운 채팅방 만들기</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="roomName">방 이름</label>
                        <input
                            type="text"
                            id="roomName"
                            value={roomName}
                            onChange={(e) => setRoomName(e.target.value)}
                            autoFocus
                        />
                    </div>
                    <div className="form-group">
                        <div className="radio-group">
                            <label>
                                <input
                                    type="radio"
                                    name="roomType"
                                    value="public"
                                    checked={!isPrivate}
                                    onChange={() => setIsPrivate(false)}
                                />
                                공개방
                            </label>
                            <label>
                                <input
                                    type="radio"
                                    name="roomType"
                                    value="private"
                                    checked={isPrivate}
                                    onChange={() => setIsPrivate(true)}
                                />
                                비밀방
                            </label>
                        </div>
                    </div>
                    {isPrivate && (
                        <div className="form-group">
                            <label htmlFor="password">비밀번호</label>
                            <input
                                type="password"
                                id="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                    )}
                    <button type="submit" className="modal-submit-btn">만들기</button>
                </form>
            </div>
        </div>
    );
}

export default CreateRoomModal;