import React, { useState } from 'react';

// 이 컴포넌트는 부모로부터 '방 만들기' 함수(onCreate)와 '모달 닫기' 함수(onClose)를 받습니다.
function CreateRoomModal({ onCreate, onClose }) {
    const [roomName, setRoomName] = useState('');
    const [roomType, setRoomType] = useState('PUBLIC');
    const [password, setPassword] = useState('');

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!roomName.trim()) {
            alert('방 이름을 입력해주세요.');
            return;
        }
        // 부모로부터 받은 onCreate 함수를 호출하여 데이터를 전달합니다.
        onCreate({ roomName, roomType, password });
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close-btn" onClick={onClose}>&times;</button>
                <h2>새 채팅방 만들기</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="room-name">방 이름</label>
                        <input
                            type="text"
                            id="room-name"
                            value={roomName}
                            onChange={(e) => setRoomName(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label>방 종류</label>
                        <div className="radio-group">
                            <label>
                                <input type="radio" value="PUBLIC" checked={roomType === 'PUBLIC'} onChange={() => setRoomType('PUBLIC')} />
                                공개방
                            </label>
                            <label>
                                <input type="radio" value="PRIVATE" checked={roomType === 'PRIVATE'} onChange={() => setRoomType('PRIVATE')} />
                                비밀방
                            </label>
                        </div>
                    </div>
                    {/* 비밀방을 선택했을 때만 비밀번호 입력창이 보입니다. */}
                    {roomType === 'PRIVATE' && (
                        <div className="form-group">
                            <label htmlFor="room-password">비밀번호</label>
                            <input
                                type="password"
                                id="room-password"
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