import React from 'react';

const SERVER_URL = 'http://localhost:8080';

// 모달 전체에 적용될 스타일
const modalStyle = {
    position: 'absolute', // 부모 요소를 기준으로 절대 위치를 가짐
    width: '250px',
    backgroundColor: 'white',
    border: '1px solid #ccc',
    borderRadius: '8px',
    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
    zIndex: 100, // 다른 채팅방 UI 위에 보이도록 z-index 설정
};

const headerStyle = {
    padding: '10px 15px',
    backgroundColor: '#f7f7f7',
    borderBottom: '1px solid #eee',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
};

const nicknameStyle = { fontWeight: 'bold', fontSize: '16px' };
const closeBtnStyle = { fontSize: '24px', color: '#aaa', cursor: 'pointer', border: 'none', background: 'none' };
const bodyStyle = { padding: '15px' };
const profilePicStyle = { width: '60px', height: '60px', borderRadius: '50%', marginRight: '15px' };
const statusMsgStyle = { marginTop: '10px', padding: '10px', backgroundColor: '#f9f9f9', borderRadius: '4px', minHeight: '40px', whiteSpace: 'pre-wrap', wordBreak: 'break-word' };

function UserProfileModal({ profile, onClose, position }) {
    if (!profile) return null;

    // position prop으로 받은 top, left를 스타일에 직접 적용
    return (
        <div style={{ ...modalStyle, top: position.top, left: position.left }}>
            <div style={headerStyle}>
                <span style={nicknameStyle}>{profile.nickname}</span>
                <button style={closeBtnStyle} onClick={onClose}>&times;</button>
            </div>
            <div style={bodyStyle}>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <img style={profilePicStyle} src={`${SERVER_URL}${profile.imageUrl}`} alt={profile.nickname} />
                    <div>
                        <p style={{ margin: 0, fontWeight: 'bold' }}>{profile.username}</p>
                    </div>
                </div>
                <div style={statusMsgStyle}>
                    {profile.status_msg || '(상태 메시지가 없습니다.)'}
                </div>
            </div>
        </div>
    );
}

export default UserProfileModal;