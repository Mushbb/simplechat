import React, { useState } from 'react';
import { toast } from 'react-toastify';
import '../styles/Modals.css';

function CreateRoomModal({ onClose, onCreate }) {
    const [roomName, setRoomName] = useState('');
    const [isPrivate, setIsPrivate] = useState(false);
    const [password, setPassword] = useState('');

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